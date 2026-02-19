package fr.labanquepostale.report.base.routes;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import fr.labanquepostale.marches.eai.core.helper.ErrorHelper;
import jakarta.annotation.Nonnull;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.labanquepostale.marches.eai.core.dollaru.DollarUUtil;
import fr.labanquepostale.marches.eai.core.helper.AuditHelper;
import fr.labanquepostale.marches.eai.core.io.FileListingUtil;
import fr.labanquepostale.marches.eai.core.io.FileListingUtil.SortCriterion;
import fr.labanquepostale.marches.eai.core.io.FileListingUtil.SortDirection;
import fr.labanquepostale.marches.eai.core.io.FileMergeUtil;
import fr.labanquepostale.marches.eai.core.io.FileMoveUtil;
import fr.labanquepostale.marches.eai.core.model.audit.Codifier;
import fr.labanquepostale.marches.eai.core.model.audit.Metadata;
import fr.labanquepostale.marches.eai.core.model.audit.Reference;
import fr.labanquepostale.marches.eai.core.model.audit.Status;

//Tibco BW process
//bw_eaireports/Process/Starters/POSITIONS_TITRES/GetExtractionDuResultatDuRapprochement.process
@Component
@SuppressWarnings("unchecked")
public class PositionTitresRoute extends RouteBuilder {
    @Autowired
    private ErrorHelper errorHelper;
    @Autowired
    private AuditHelper auditHelper;

    @Override
    public void configure() {

        // --- CATCH-ALL ---
        onException(Exception.class)
                .handled(true)
                .process(this::handleException);

        fromF("file:{{eai.report.position.titres.input.dir}}"
                + "?include={{eai.report.position.titres.input.notification.name}}"
                + "&delay={{eai.report.position.titres.input.delay.ms}}"
                + "&noop=true"
                + "&idempotent=false"
                + "&readLock=changed")
                .routeId("PositionTitresRoute")

                // --- SAVE NOTIFICATION FILE PATH ---
                .setProperty("notifPath", header("CamelFilePath"))

                // --- CACHE PARAMETERS AS EXCHANGE PROPERTIES (EASY TO REUSE) ---
                .setProperty("notificationContent", simple("${body}"))
                .setProperty("notifcationFileName", simple("{{eai.report.position.titres.input.notification.name}}"))
                .setProperty("dollarURessourceName", simple("{{eai.report.position.titres.input.notification.name}}"))
                .setProperty("inputDir", simple("{{eai.report.position.titres.input.dir}}"))
                .setProperty("filePattern", simple("{{eai.report.position.titres.input.file.pattern}}"))
                .setProperty("fileExtension", simple("{{eai.report.position.titres.input.file.extension}}"))
                .setProperty("outputDir", simple("{{eai.report.position.titres.output.dir}}"))
                .setProperty("outputPrefix", simple("{{eai.report.position.titres.output.file.prefix}}"))
                .setProperty("archivesSubdir", simple("{{eai.report.position.titres.input.dir.archives.subdir}}"))
                .setProperty("errorsSubdir", simple("{{eai.report.position.titres.input.dir.errors.subdir}}"))
                .setProperty("listName", simple("{{eai.report.position.titres.mailing.list}}"))


                .process(exchange -> {
                    String filePatten = exchange.getProperty("filePattern", String.class) + "_" + StringUtils.substringAfterLast(exchange.getProperty("notificationContent", String.class), "=") + exchange.getProperty("fileExtension", String.class);
                    FileListingUtil.listFiles(exchange, Path.of(exchange.getProperty("inputDir", String.class)), false, SortCriterion.NAME, SortDirection.ASC, filePatten);
                })
                .choice()
                .when(exchangeProperty("inputFiles").isNull())
                // --- AUCUN FICHIER ---
                .process(exchange -> {
                    auditHelper.audit(exchange)
                            .ref(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("notifcationFileName", String.class))
                            .desc("Notification position titre reçue mais aucun fichier n'a été détecté")
                            .status(Status.Info.getStatus())
                            .data(exchange.getProperty("notificationContent"))
                            .meta("PROCESS_NAME", this.getClass().getName())
                            .send();
                })
                // --- DELETE NOTIFICATION ---
                .process(this::deleteNotification)
                .log("Aucun fichier détecté avec le pattern ${exchangeProperty.filePattern} dans le repertoire mergedFile: ${exchangeProperty.inputDir}")
                .stop()

                .otherwise()
                .process(exchange -> {
                    auditHelper.audit(exchange)
                            .ref(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("notifcationFileName", String.class))
                            .desc("Fichiers Position titres MO Calypso reçu")
                            .status(Status.Info.getStatus())
                            .data(exchange.getProperty("inputFileNames", String.class))
                            .meta("PROCESS_NAME", this.getClass().getName())
                            .meta(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("notifcationFileName", String.class))
                            .send();
                })

                .process(exchange -> {
                    String outputFilePath = exchange.getProperty("outputDir", String.class) + exchange.getProperty("outputPrefix", String.class) + "_" + StringUtils.substringAfterLast(exchange.getProperty("notificationContent", String.class), "=") + exchange.getProperty("fileExtension", String.class);
                    String outputFileName = exchange.getProperty("outputPrefix", String.class) + "_" + StringUtils.substringAfterLast(exchange.getProperty("notificationContent", String.class), "=") + exchange.getProperty("fileExtension", String.class);
                    exchange.setProperty("outputFilePath", outputFilePath);
                    exchange.setProperty("outputFileName", outputFileName);
                    FileMergeUtil.merge(Path.of(exchange.getProperty("outputFilePath", String.class)), (List<Path>) exchange.getProperty("inputFiles"), 65536, false);
                })

                // --- SEND MAIL ---
                .process(exchange -> {
                    exchange.getMessage().setHeader("listName", exchange.getProperty("listName", String.class));
                    exchange.getMessage().setHeader("subject", "Rapport");

                    // --- CONTENT-TYPE DU BODY ---
                    String templatePath = exchange.getContext().resolvePropertyPlaceholders("{{eai.report.position.titres.mailing.template}}");
                    String htmlBody = Files.readString(Path.of(templatePath), StandardCharsets.UTF_8);
                    exchange.getMessage().setBody(htmlBody);
                    exchange.getMessage().setHeader("Content-Type", "text/html; charset=UTF-8");
                    //exchange.getMessage().setBody("Bonjour, rapport en pièce jointe.");

                    // --- attached files
                    var am = exchange.getMessage(org.apache.camel.attachment.AttachmentMessage.class);
                    var file = new java.io.File(exchange.getProperty("outputFilePath", String.class));
                    var ds = new jakarta.activation.FileDataSource(file);
                    am.addAttachment(exchange.getProperty("outputFileName", String.class), new jakarta.activation.DataHandler(ds));
                })
                .to("direct:MailRoute")

                // --- DollarU OK ---
                .process(exchange -> {
                    List<Metadata> metadatas = new ArrayList<Metadata>();
                    metadatas.add(new Metadata(Codifier.PROCESS_NAME.getCodifier(), this.getClass().getName()));
                    List<Reference> references = new ArrayList<Reference>();
                    references.add(new Reference(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("notifcationFileName", String.class)));
                    exchange.getMessage().setBody(DollarUUtil.buildOKDollarURequest(exchange.getProperty("dollarURessourceName", String.class), references, metadatas));
                }).to("direct:dollarUCommand")
                // --- ARCHIVE FILES ---
                .process(exchange -> {
                    //moveListedFiles(exchange, exchange.getProperty("archivesSubdir", String.class));
                    FileMoveUtil.move(exchange, exchange.getProperty("archivesSubdir", String.class), "inputFiles");
                })
                // --- AUDIT OK ---
                .process(exchange -> {
                    auditHelper.audit(exchange)
                            .ref(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("notifcationFileName", String.class))
                            .desc("POSITIONS_TITRES rapports Calypso traités")
                            .status(Status.Success.getStatus())
                            .data("")
                            .meta("PROCESS_NAME", this.getClass().getName())
                            .meta(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("notifcationFileName", String.class))
                            .send();
                })
                // --- DELETE NOTIFICATION ---
                .process(this::deleteNotification)
                .log("Merge OK: mergedFile=${exchangeProperty.outputFilePath}");
    }


    private void deleteNotification(Exchange e) throws Exception {
        String notifPathStr = e.getProperty("notifPath", String.class);
        if (notifPathStr == null) {
            return;
        }
        Files.deleteIfExists(Paths.get(notifPathStr));
    }


    private void handleException(@Nonnull Exchange exchange) throws Exception {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        //Audit exception
        errorHelper.error(exchange, exception)
                .ref(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("inputFileName", String.class))
                .meta(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("inputFileName", String.class))
                .meta(Codifier.PROCESS_NAME.getCodifier(), this.getClass().getName())
                .send();

        List<Metadata> metadatas = new ArrayList<Metadata>();
        metadatas.add(new Metadata(Codifier.PROCESS_NAME.getCodifier(), this.getClass().getName()));
        List<Reference> references = new ArrayList<Reference>();
        references.add(new Reference(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("inputFileName", String.class)));
        exchange.getMessage().setBody(DollarUUtil.buildNOKDollarURequest(exchange.getProperty("dollarURessourceName", String.class), references, metadatas));
        exchange.getContext().createProducerTemplate().send("direct:dollarUCommand", exchange);
        FileMoveUtil.move(exchange, exchange.getProperty("errorsSubdir", String.class), "inputFiles");
        deleteNotification(exchange);
        log.error("DollarU NOK: reason={}", (exception != null ? exception.getMessage() : "unknown"), exception);
    }
}
