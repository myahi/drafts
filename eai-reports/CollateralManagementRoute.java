package fr.labanquepostale.report.collateral.routes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.labanquepostale.marches.eai.core.dollaru.DollarUUtil;
import fr.labanquepostale.marches.eai.core.file.FileMoveUtil;
import fr.labanquepostale.marches.eai.core.helper.AuditHelper;
import fr.labanquepostale.marches.eai.core.model.audit.Codifier;
import fr.labanquepostale.marches.eai.core.model.audit.Status;

@Component
public class CollateralManagementRoute extends RouteBuilder {

    @Autowired
    private AuditHelper auditHelper;

    @Override
    public void configure() {

        // Catch-all
        onException(Exception.class)
            .handled(true)
            .process(this::handleException);

        fromF("file:{{eai.report.collateral.input.dir}}"
            + "?include={{eai.report.collateral.input.file.pattern}}"
            + "&delay={{eai.report.collateral.input.delay.ms}}"
            + "&readLock=changed"
            + "&idempotent=true")
        .routeId("eai-camel-report-collateral-management-route")

        // Cache context
        .setProperty("inputDir", simple("{{eai.report.collateral.input.dir}}"))
        .setProperty("archivesSubdir", simple("{{eai.report.collateral.input.dir.archives.subdir}}"))
        .setProperty("errorsSubdir", simple("{{eai.report.collateral.input.dir.errors.subdir}}"))
        .setProperty("outputDir", simple("{{eai.report.collateral.output.dir}}"))
        .setProperty("dollarURessourceName", simple("{{eai.report.collateral.dollaru.ressource}}"))

        // File info
        .setProperty("inputFilePath", header("CamelFilePath"))
        .setProperty("inputFileName", header("CamelFileName"))

        // Audit réception
        .process(exchange -> {
            auditHelper.audit(exchange)
                .ref(
                    Codifier.FILE_NAME.getCodifier(),
                    exchange.getProperty("inputFileName", String.class)
                )
                .desc("Fichier Collateral Management détecté")
                .status(Status.Info.getStatus())
                .data("")
                .meta("PROCESS_NAME", this.getClass().getName())
                .send();
        })

        // Traitement métier
        .process(this::removeLastLineAndCopy)

        // DollarU OK
        .process(exchange ->
            exchange.getMessage().setBody(
                DollarUUtil.buildOKDollarURequest(
                    exchange.getProperty("dollarURessourceName", String.class)
                )
            )
        )
        .to("direct:dollarUCommand")

        // Archivage du fichier source
        .process(exchange ->
            FileMoveUtil.move(exchange, "archivesSubdir", "inputFilePath")
        )

        // Audit succès
        .process(exchange -> {
            auditHelper.audit(exchange)
                .ref(
                    Codifier.FILE_NAME.getCodifier(),
                    exchange.getProperty("inputFileName", String.class)
                )
                .desc("Fichier Collateral Management traité")
                .status(Status.Success.getStatus())
                .data("")
                .meta("PROCESS_NAME", this.getClass().getName())
                .send();
        })

        .log("Collateral management OK: file=${exchangeProperty.inputFileName}");
    }

    /**
     * Supprime la dernière ligne du fichier détecté
     * et dépose le fichier modifié dans le répertoire cible.
     */
    private void removeLastLineAndCopy(Exchange exchange) throws Exception {
        Path inputFile = Paths.get(exchange.getProperty("inputFilePath", String.class));
        Path outputDir = Paths.get(exchange.getProperty("outputDir", String.class));

        Files.createDirectories(outputDir);

        List<String> lines = Files.readAllLines(inputFile);
        if (!lines.isEmpty()) {
            lines.remove(lines.size() - 1);
        }

        Path target = outputDir.resolve(inputFile.getFileName());
        Files.write(target, lines);

        exchange.setProperty("outputFile", target.toString());
    }

    /**
     * Gestion des erreurs :
     * - DollarU NOK
     * - déplacement du fichier en erreur
     */
    private void handleException(Exchange exchange) throws Exception {
        Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);

        exchange.getMessage().setBody(
            DollarUUtil.buildNOKDollarURequest(
                exchange.getProperty("dollarURessourceName", String.class)
            )
        );
        exchange.getContext()
            .createProducerTemplate()
            .send("direct:dollarUCommand", exchange);

        // Move en erreur
        FileMoveUtil.move(exchange, "errorsSubdir", "inputFilePath");

        log.error(
            "Collateral management NOK: reason={}",
            (ex != null ? ex.getMessage() : "unknown"),
            ex
        );
    }
}
