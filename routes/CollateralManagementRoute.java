package fr.labanquepostale.report.base.routes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.labanquepostale.marches.eai.core.dollaru.DollarUUtil;
import fr.labanquepostale.marches.eai.core.helper.AuditHelper;
import fr.labanquepostale.marches.eai.core.io.FileMoveUtil;
import fr.labanquepostale.marches.eai.core.model.audit.Codifier;
import fr.labanquepostale.marches.eai.core.model.audit.Metadata;
import fr.labanquepostale.marches.eai.core.model.audit.Reference;
import fr.labanquepostale.marches.eai.core.model.audit.Status;

//Tibco BW process
//bw_eaireports/Process/Starters/get Source for COLLATERAL MANAGEMENT.process
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

        fromF("file:{{eai.report.collateral.management.input.dir}}"
            + "?include={{eai.report.collateral.management.input.file.pattern}}"
            + "&delay={{eai.report.collateral.management.input.delay.ms}}"
            + "&readLock=rename"
            + "&noop=true&idempotent=false")
        .routeId("CollateralManagementRoute")

        // Cache context
        .setProperty("inputDir", simple("{{eai.report.collateral.management.input.dir}}"))
        .setProperty("archivesSubdir", simple("{{eai.report.collateral.management.input.dir.archives.subdir}}"))
        .setProperty("errorsSubdir", simple("{{eai.report.collateral.management.input.dir.errors.subdir}}"))
        .setProperty("outputDir", simple("{{eai.report.collateral.management.output.dir}}"))
        .setProperty("dollarURessourceName", simple("{{eai.report.collateral.management.dollaru.ressource.name}}"))

        // File info
        .setProperty("inputFilePath", header("CamelFilePath"))
        .setProperty("inputFileName", header("CamelFileName"))

        // Audit réception
        .process(exchange -> {
            auditHelper.audit(exchange)
                .ref(Codifier.FILE_NAME.getCodifier(),exchange.getProperty("inputFileName", String.class))
                .desc("Fichier Collateral Management détecté")
                .status(Status.Info.getStatus())
                .data("")
                .meta(Codifier.PROCESS_NAME.getCodifier(), this.getClass().getName())
                .send();
        })

        // Traitement métier
        .process(this::removeLastLineAndCopy)

        // DollarU OK
        .process(exchange -> {
        	List<Metadata> metadatas = new ArrayList<Metadata>();
        	metadatas.add(new Metadata(Codifier.PROCESS_NAME.getCodifier(),this.getClass().getName()));
        	List<Reference> references = new ArrayList<Reference>();
        	references.add(new Reference(Codifier.FILE_NAME.getCodifier(),exchange.getProperty("inputFileName", String.class)));
        	exchange.getMessage().setBody(DollarUUtil.buildOKDollarURequest(exchange.getProperty("dollarURessourceName", String.class),references,metadatas));
        })
        .to("direct:dollarUCommand")

        // Archivage du fichier source
        .process(exchange -> FileMoveUtil.move(exchange, "archivesSubdir", "inputFilePath"))

        // Audit succès
        .process(exchange -> {
            auditHelper.audit(exchange)
                .ref(Codifier.FILE_NAME.getCodifier(),exchange.getProperty("inputFileName", String.class))
                .desc("Fichier Collateral Management traité")
                .status(Status.Success.getStatus())
                .data("")
                .meta("PROCESS_NAME", this.getClass().getName())
                .send();
        })

        .log("Fichier Collateral Management Calypso traité: file=${exchangeProperty.inputFileName}");
    }

    /**
     * Delete last line of file and move the result to the target folder
     */
    private void removeLastLineAndCopy(Exchange exchange) throws Exception {
        Path inputFile = Paths.get(exchange.getProperty("inputFilePath", String.class));
        Path outputDir = Paths.get(exchange.getProperty("outputDir", String.class));

        Files.createDirectories(outputDir);

        List<String> lines = Files.readAllLines(inputFile);
        if (!lines.isEmpty()) {
            lines.remove(lines.size() - 2);
        }

        Path target = outputDir.resolve(inputFile.getFileName());
        Files.write(target, lines);

        exchange.setProperty("outputFile", target.toString());
    }

    private void handleException(Exchange exchange) throws Exception {
        Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        List<Metadata> metadatas = new ArrayList<Metadata>();
    	metadatas.add(new Metadata(Codifier.PROCESS_NAME.getCodifier(),this.getClass().getName()));
    	List<Reference> references = new ArrayList<Reference>();
    	references.add(new Reference(Codifier.FILE_NAME.getCodifier(),exchange.getProperty("inputFileName", String.class)));
        exchange.getMessage().setBody(DollarUUtil.buildNOKDollarURequest(exchange.getProperty("dollarURessourceName", String.class),references,metadatas));
        exchange.getContext().createProducerTemplate().send("direct:dollarUCommand", exchange);
        // Move en erreur
        FileMoveUtil.move(exchange, "errorsSubdir", "inputFilePath");
        log.error("Collateral management NOK: reason={}",(ex != null ? ex.getMessage() : "unknown"),ex);
    }
}
