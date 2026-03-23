package fr.labanquepostale.report.base.routes.valo.pool3g;

import fr.labanquepostale.marches.eai.core.dollaru.DollarUUtil;
import fr.labanquepostale.marches.eai.core.helper.AuditHelper;
import fr.labanquepostale.marches.eai.core.helper.ErrorHelper;
import fr.labanquepostale.marches.eai.core.io.FileMoveUtil;
import fr.labanquepostale.marches.eai.core.model.audit.Codifier;
import fr.labanquepostale.marches.eai.core.model.audit.Metadata;
import fr.labanquepostale.marches.eai.core.model.audit.Reference;
import fr.labanquepostale.marches.eai.core.model.audit.Status;
import fr.labanquepostale.report.base.beans.pool3g.ValoPool3GBean;
import fr.labanquepostale.report.base.model.pool3g.CalypsoLine;
import fr.labanquepostale.report.base.model.pool3g.SfdhLine;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

//Tibco BW process
//bw_eaireports/Process/Starters/VALO_POOL3G/get Source for Valo_Pool3G_M.process
@Component
public class ValoPool3GMonthlyRoute extends RouteBuilder {
    @Autowired
    private AuditHelper auditHelper;
    @Autowired
    private ErrorHelper errorHelper;

    private final ValoPool3GBean bean;

    public ValoPool3GMonthlyRoute(ValoPool3GBean proc) {
        this.bean = proc;
    }

    @Override
    public void configure() {

        // Catch-all
        onException(Exception.class)
                .handled(true)
                .process(this::handleException);

        BindyCsvDataFormat inCsv = new BindyCsvDataFormat(CalypsoLine.class);
        BindyCsvDataFormat outCsv = new BindyCsvDataFormat(SfdhLine.class);

        fromF("file:{{eai.report.valo.pool3g.monthly.input.dir}}"
                + "?include={{eai.report.valo.pool3g.monthly.input.file.pattern}}"
                + "&delay={{eai.report.valo.pool3g.monthly.input.delay.ms}}"
                + "&noop=true"
                + "&idempotent=false"
                + "&readLock=changed")
                .routeId("ValoPool3GMonthly")
                // Cache context
                .setProperty("auditRef", simple("${date:now:ddMMyyyyHHmmss}"))
                .setProperty("inputDir", simple("{{eai.report.valo.pool3g.monthly.input.dir}}"))
                .setProperty("archivesSubdir", simple("{{eai.report.valo.pool3g.monthly.input.dir.archives.subdir}}"))
                .setProperty("errorsSubdir", simple("{{eai.report.valo.pool3g.monthly.input.dir.errors.subdir}}"))
                .setProperty("outputDir", simple("{{eai.report.valo.pool3g.monthly.output.dir}}"))
                .setProperty("dollarURessourceName", simple("{{eai.report.valo.pool3g.monthly.dollaru.ressource.name}}"))
                // File info
                .setProperty("inputFilePath", header("CamelFilePath"))
                .setProperty("inputFileName", header("CamelFileName"))
                // Audit réception
                .process(exchange -> {
                    auditHelper.audit(exchange)
                            .ref(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("inputFileName", String.class))
                            .desc("Fichier quotidien Valo Pool3G de Calypso - Début de traitement")
                            .status(Status.Info.getStatus())
                            .data("")
                            .meta(Codifier.PROCESS_NAME.getCodifier(), this.getClass().getName())
                            .meta(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("inputFileName", String.class))
                            .send();
                })
                //Process
                .unmarshal(inCsv)
                .bean(bean,"groupByCodeBom")
                .marshal(outCsv)
                .to("file:{{eai.report.valo.pool3g.monthly.output.dir}}?fileName=${exchangeProperty.inputFileName}")
                /// DollarU OK
                .process(exchange -> {
                    List<Metadata> metadatas = new ArrayList<Metadata>();
                    metadatas.add(new Metadata(Codifier.PROCESS_NAME.getCodifier(),this.getClass().getName()));
                    metadatas.add(new Metadata(Codifier.FILE_NAME.getCodifier(),exchange.getProperty("inputFileName", String.class)));
                    List<Reference> references = new ArrayList<Reference>();
                    references.add(new Reference(Codifier.DATE.getCodifier(), exchange.getProperty("auditRef",String.class)));
                    exchange.getMessage().setBody(DollarUUtil.buildOKDollarURequest(exchange.getProperty("dollarURessourceName", String.class),references,metadatas));
                }).to("direct:dollarUCommand")
                // Arch
                .process(exchange -> {
                    FileMoveUtil.move(exchange, exchange.getProperty("archivesSubdir", String.class), "inputFilePath");
                })
                //Audit fin de traitement
                .process(exchange -> {
                    auditHelper.audit(exchange)
                            .ref(Codifier.DATE.getCodifier(), exchange.getProperty("auditRef", String.class))
                            .desc("Fichier quotidien Valo Pool3G de Calypso transformé et envoyé à SFDH")
                            .status(Status.Success.getStatus())
                            .data("")
                            .meta(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("inputFileName", String.class))
                            .meta("PROCESS_NAME", this.getClass().getName())
                            .meta(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("inputFileName", String.class))
                            .send();
                });
    }
    private void handleException(Exchange exchange) throws Exception {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        errorHelper.error(exchange, exception)
                .ref(Codifier.DATE.getCodifier(), exchange.getProperty("auditRef", String.class))
                .meta(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("inputFileName", String.class))
                .meta(Codifier.PROCESS_NAME.getCodifier(), this.getClass().getName())
                .send();

        List<Metadata> metadatas = new ArrayList<Metadata>();
        metadatas.add(new Metadata(Codifier.PROCESS_NAME.getCodifier(),this.getClass().getName()));
        metadatas.add(new Metadata(Codifier.FILE_NAME.getCodifier(),exchange.getProperty("inputFileName", String.class)));
        List<Reference> references = new ArrayList<Reference>();
        references.add(new Reference(Codifier.FILE_NAME.getCodifier(),exchange.getProperty("inputFileName", String.class)));
        exchange.getMessage().setBody(DollarUUtil.buildNOKDollarURequest(exchange.getProperty("dollarURessourceName", String.class),references,metadatas));
        exchange.getContext()
                .createProducerTemplate()
                .send("direct:dollarUCommand", exchange);
        FileMoveUtil.move(exchange, exchange.getProperty("errorsSubdir", String.class), "inputFilePath");
        log.error("Merge NOK: reason={}", (exception != null ? exception.getMessage() : "unknown"), exception);
    }
}
