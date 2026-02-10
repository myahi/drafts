package fr.labanquepostale.report.base.routes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.labanquepostale.marches.eai.core.dollaru.DollarUUtil;
import fr.labanquepostale.marches.eai.core.helper.AuditHelper;
import fr.labanquepostale.marches.eai.core.io.FileMergeUtil;
import fr.labanquepostale.marches.eai.core.model.audit.Codifier;
import fr.labanquepostale.marches.eai.core.model.audit.Metadata;
import fr.labanquepostale.marches.eai.core.model.audit.Reference;
import fr.labanquepostale.marches.eai.core.model.audit.Status;

//Tibco BW process
//bw_eaireports/Process/Starters/POSITIONS_TITRES/GetExtractionDuResultatDuRapprochement.process
@Component
@SuppressWarnings("unchecked")
public class PositionTitresRoute extends RouteBuilder {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private AuditHelper auditHelper;
    
    @Override
    public void configure() {

    	// Catch-all        
        onException(Exception.class)
        .handled(true)
        .process(this::handleException);

        fromF("file:{{eai.report.position.titres.input.dir}}"
                + "?include={{eai.report.position.titres.input.notification.name}}"
                + "&delay={{eai.report.position.titres.input.delay.ms}}"
                + "&noop=true"
                + "&idempotent=true"
                + "&readLock=changed")
        	.routeId("CashFlowsRoute")

            // Save notification file path for cleanup
            .setProperty("notifPath", header("CamelFilePath"))

            // Cache parameters as exchange properties (easy to reuse)
            .setProperty("notifcationFileName", simple("{{eai.report.position.titres.input.notification.name}}"))
            .setProperty("dollarURessourceName", simple("{{eai.report.position.titres.input.notification.name}}"))
            .setProperty("inputDir", simple("{{eai.report.position.titres.input.dir}}"))
            .setProperty("filePattern", simple("{{eai.report.position.titres.input.file.pattern}}"))
            .setProperty("outputDir", simple("{{eai.report.position.titres.output.dir}}"))
            .setProperty("outputPrefix", simple("{{eai.report.position.titres.output.file.prefix}}"))
            .setProperty("archivesSubdir", simple("{{eai.report.position.titres.input.dir.archives.subdir}}"))
            .setProperty("errorsSubdir", simple("{{eai.report.position.titres.input.dir.errors.subdir}}"))
            
            .process(this::listFilesToMerge)
            .choice()
            .when(exchangeProperty("intputFiles").isNull())
                // --- AUCUN FICHIER ---
                .process(exchange -> {
                    auditHelper.audit(exchange)
                        .ref(Codifier.FILE_NAME.getCodifier(),exchange.getProperty("notifcationFileName", String.class))
                        .desc("Notification cashflows MO reçu mais aucun fichier cashflow détecté")
                        .status(Status.Info.getStatus())
                        .data("")
                        .meta("PROCESS_NAME", this.getClass().getName())
                        .send();
                })
                .process(this::deleteNotification)
                .log("Aucun fichier détecté avec le pattern ${exchangeProperty.filePattern} dans le repertoire mergedFile: ${exchangeProperty.inputDir}")
                .stop()
             
            .otherwise()
            .process(exchange->{
	        	 auditHelper.audit(exchange)
	        	.ref(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("notifcationFileName",String.class))
               .desc("Fichiers cashflows MO Calypso reçu")
               .status(Status.Info.getStatus())
               .data(exchange.getProperty("filesToMergeAsString",String.class))
               .meta("PROCESS_NAME", this.getClass().getName())
               .meta(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("notifcationFileName",String.class))
               .send();
	         })
            .process(this::mergeFiles)
                        
            // DollarU OK
            .process(exchange -> {
            	List<Metadata> metadatas = new ArrayList<Metadata>();
            	metadatas.add(new Metadata(Codifier.PROCESS_NAME.getCodifier(),this.getClass().getName()));
            	List<Reference> references = new ArrayList<Reference>();
            	references.add(new Reference(Codifier.FILE_NAME.getCodifier(),exchange.getProperty("inputFileName", String.class)));
            	exchange.getMessage().setBody(DollarUUtil.buildOKDollarURequest(exchange.getProperty("dollarURessourceName", String.class),references,metadatas));
            })
            .to("direct:dollarUCommand")
            
			.process(exchange -> moveListedFiles(exchange, exchange.getProperty("archivesSubdir", String.class)))
            
            .process(this::deleteNotification)
            
            .process(exchange->{
	            auditHelper.audit(exchange)
	           .ref(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("notifcationFileName",String.class))
              .desc("Fichier CashFlows MO Calypso traité")
              .status(Status.Success.getStatus())
              .data("")
              .meta("PROCESS_NAME", this.getClass().getName())
              .meta(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("notifcationFileName",String.class))
              .send();
	         })
            
            .log("Merge OK: mergedFile=${exchangeProperty.outputFile}");
    }

    private void listFilesToMerge(Exchange exchange) throws Exception {
    	   String inputDir = exchange.getProperty("inputDir", String.class);
    	    String regex = exchange.getProperty("filePattern", String.class);

    	    Pattern pattern = Pattern.compile(regex);

    	    try (Stream<Path> stream = Files.list(Paths.get(inputDir))) {
    	        List<Path> files = stream
    	            .filter(Files::isRegularFile)
    	            .filter(path -> pattern.matcher(path.getFileName().toString()).matches())
    	            .sorted()
    	            .collect(Collectors.toList());

    	        exchange.setProperty("intputFiles", files);

    	        String filesAsString = files.stream()
    	            .map(Path::getFileName)
    	            .map(Path::toString)
    	            .collect(Collectors.joining("\n"));

    	        exchange.setProperty("filesToMergeAsString", filesAsString);
    	    }
    }

    private void mergeFiles(Exchange e) throws Exception {
        List<Path> files = (List<Path>) e.getProperty("intputFiles");

        String outputDir = e.getProperty("outputDir", String.class);
        String prefix = e.getProperty("outputPrefix", String.class);

        String date = LocalDate.now().format(YYYYMMDD);
        Path target = Paths.get(outputDir, prefix + date + ".csv");
        FileMergeUtil.merge(target, files, HIGHEST, false);
        e.setProperty("outputFile", target.toString());
    }

    private void moveListedFiles(Exchange e, String subdir) throws Exception {
        List<Path> files = (List<Path>) e.getProperty("intputFiles");
        if (files == null || files.isEmpty()) {
            return;
        }

        String inputDir = e.getProperty("inputDir", String.class);
        Path destDir = Paths.get(inputDir, subdir);
        Files.createDirectories(destDir);

        for (Path src : files) {
            if (Files.exists(src)) {
                Path dest = destDir.resolve(src.getFileName());
                Files.move(src, dest);
            }
        }
    }

    private void deleteNotification(Exchange e) throws Exception {
        String notifPathStr = e.getProperty("notifPath", String.class);
        if (notifPathStr == null) {
            return;
        }
        Files.deleteIfExists(Paths.get(notifPathStr));
    }

    
    /**
     * Handles any exception raised in the route.
     * Responsibilities:
     * - send DollarU NOK (flowType + "_NOK", quantity=1)
     * - delete notification file
     * - move listed files (if any) to errors subdirectory
     */
    private void handleException(Exchange exchange) throws Exception {
        Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);

        List<Metadata> metadatas = new ArrayList<Metadata>();
    	metadatas.add(new Metadata(Codifier.PROCESS_NAME.getCodifier(),this.getClass().getName()));
    	List<Reference> references = new ArrayList<Reference>();
    	references.add(new Reference(Codifier.FILE_NAME.getCodifier(),exchange.getProperty("inputFileName", String.class)));
        exchange.getMessage().setBody(DollarUUtil.buildNOKDollarURequest(exchange.getProperty("dollarURessourceName", String.class),references,metadatas));
        exchange.getContext().createProducerTemplate().send("direct:dollarUCommand", exchange);
        deleteNotification(exchange);
        moveListedFiles(exchange, exchange.getProperty("errorsSubdir", String.class));
        log.error("Merge NOK: reason={}", (ex != null ? ex.getMessage() : "unknown"), ex);
    }
}
