package fr.labanquepostale.report.base.routes.acadia;

import fr.labanquepostale.marches.eai.core.dollaru.DollarUUtil;
import fr.labanquepostale.marches.eai.core.helper.AuditHelper;
import fr.labanquepostale.marches.eai.core.helper.ErrorHelper;
import fr.labanquepostale.marches.eai.core.io.FileMoveUtil;
import fr.labanquepostale.marches.eai.core.model.audit.Codifier;
import fr.labanquepostale.marches.eai.core.model.audit.Metadata;
import fr.labanquepostale.marches.eai.core.model.audit.Reference;
import fr.labanquepostale.marches.eai.core.model.audit.Status;
import fr.labanquepostale.report.base.beans.acadia.PrepareFileForAcadiaBean;
import fr.labanquepostale.report.base.model.acadia.AcadiaIceLine;
import fr.labanquepostale.report.base.model.acadia.CsoReportLine;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class PrepareFileForAcadiaRoute extends RouteBuilder {

    private static final String PROCESS_NAME = PrepareFileForAcadiaRoute.class.getName();
    private static final String OUTPUT_HEADER =
            "ValuationDate\tPortfolioID\tTradeID\tEndDate\tIMModel\tPostRegulations\tCollectRegulations\tProductClass\tSensitivity_Id\tRiskType\tQualifier\tBucket\tLabel1\tLabel2\tAmount\tAmountCurrency\tAmountUSD";

    @Autowired
    private AuditHelper auditHelper;

    @Autowired
    private ErrorHelper errorHelper;

    private final PrepareFileForAcadiaBean bean;

    public PrepareFileForAcadiaRoute(PrepareFileForAcadiaBean bean) {
        this.bean = bean;
    }

    @Override
    public void configure() {

        onException(Exception.class)
                .handled(true)
                .process(this::handleException);

        BindyCsvDataFormat csoCsv = new BindyCsvDataFormat(CsoReportLine.class);
        BindyCsvDataFormat iceCsv = new BindyCsvDataFormat(AcadiaIceLine.class);

        fromF("file:{{eai.report.acadia.prepare.input.cso.dir}}"
                + "?include={{eai.report.acadia.prepare.input.cso.file.pattern}}"
                + "&delay={{eai.report.acadia.prepare.input.delay.ms}}"
                + "&noop=true"
                + "&idempotent=false"
                + "&readLock=changed")
                .routeId("PrepareFileForAcadia")

                // Contexte global
                .setProperty("auditRef", simple("${date:now:ddMMyyyyHHmmss}"))
                .setProperty("inputCsoDir", simple("{{eai.report.acadia.prepare.input.cso.dir}}"))
                .setProperty("inputIceDir", simple("{{eai.report.acadia.prepare.input.ice.dir}}"))
                .setProperty("archivesSubdir", simple("{{eai.report.acadia.prepare.input.dir.archives.subdir}}"))
                .setProperty("errorsSubdir", simple("{{eai.report.acadia.prepare.input.dir.errors.subdir}}"))
                .setProperty("outputDir", simple("{{eai.report.acadia.prepare.output.dir}}"))
                .setProperty("dollarURessourceName", simple("{{eai.report.acadia.prepare.dollaru.ressource.name}}"))
                .setProperty("iceEncoding", simple("{{eai.report.acadia.prepare.input.ice.encoding}}"))
                .setProperty("icePattern", simple("{{eai.report.acadia.prepare.input.ice.file.pattern}}"))
                .setProperty("outputExtension", simple("{{eai.report.acadia.prepare.output.file.extension}}"))

                // Fichier CSO
                .setProperty("csoInputFilePath", header("CamelFilePath"))
                .setProperty("csoInputFileName", header("CamelFileName"))

                // Audit début traitement CSO
                .process(exchange -> {
                    auditHelper.audit(exchange)
                            .ref(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("csoInputFileName", String.class))
                            .desc("CSO Report ACADIA - Début de traitement")
                            .status(Status.Info.getStatus())
                            .data("")
                            .meta(Codifier.PROCESS_NAME.getCodifier(), PROCESS_NAME)
                            .meta(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("csoInputFileName", String.class))
                            .send();
                })

                // Parse CSO + mapping TradeID -> ExternalTradeID
                .unmarshal(csoCsv)
                .bean(bean, "buildTradeIdMapping")
                .setProperty("tradeIdMapping", body())

                // Liste les fichiers ICE à traiter
                .process(exchange -> {
                    List<String> files = bean.listIceFiles(
                            exchange.getProperty("inputIceDir", String.class),
                            exchange.getProperty("icePattern", String.class),
                            exchange.getProperty("csoInputFileName", String.class)
                    );
                    exchange.getIn().setBody(files);
                })

                .split(body()).stopOnException()

                    .setProperty("inputFilePath", body())
                    .process(exchange -> {
                        String inputFilePath = exchange.getProperty("inputFilePath", String.class);
                        String inputFileName = bean.extractFileName(inputFilePath);

                        exchange.setProperty("inputFileName", inputFileName);
                    })

                    // Lecture brute du fichier ICE
                    .process(exchange -> {
                        String raw = bean.readFile(
                                exchange.getProperty("inputFilePath", String.class),
                                exchange.getProperty("iceEncoding", String.class)
                        );
                        exchange.getIn().setBody(raw);
                    })

                    // Audit début fichier ICE
                    .process(exchange -> {
                        auditHelper.audit(exchange)
                                .ref(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("inputFileName", String.class))
                                .desc("Fichier ICE pour ACADIA - Début de traitement")
                                .status(Status.Info.getStatus())
                                .data("")
                                .meta(Codifier.PROCESS_NAME.getCodifier(), PROCESS_NAME)
                                .meta(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("inputFileName", String.class))
                                .send();
                    })

                    // Parse ICE
                    .unmarshal(iceCsv)

                    // Transformation
                    .process(exchange -> {
                        @SuppressWarnings("unchecked")
                        List<AcadiaIceLine> lines = exchange.getIn().getBody(List.class);

                        @SuppressWarnings("unchecked")
                        Map<String, String> tradeIdMapping =
                                exchange.getProperty("tradeIdMapping", Map.class);

                        String inputFileName = exchange.getProperty("inputFileName", String.class);

                        List<AcadiaIceLine> transformed = bean.transformIceLines(
                                lines,
                                tradeIdMapping,
                                bean.extractPortfolioIdFromFileName(inputFileName)
                        );

                        exchange.getIn().setBody(transformed);
                    })

                    // Rendu du body final
                    .bean(bean, "toOutputBody")
                    .setBody(simple(OUTPUT_HEADER + "\n${body}"))

                    // Nom du fichier de sortie
                    .setProperty("outputFileName", method(bean,
                            "buildOutputFileName(${exchangeProperty.inputFileName}, ${exchangeProperty.outputExtension})"))

                    // Ecriture
                    .toD("file:${exchangeProperty.outputDir}?fileName=${exchangeProperty.outputFileName}")

                    // DollarU OK
                    .process(exchange -> {
                        List<Metadata> metadatas = new ArrayList<>();
                        metadatas.add(new Metadata(Codifier.PROCESS_NAME.getCodifier(), PROCESS_NAME));
                        metadatas.add(new Metadata(Codifier.FILE_NAME.getCodifier(),
                                exchange.getProperty("inputFileName", String.class)));

                        List<Reference> references = new ArrayList<>();
                        references.add(new Reference(Codifier.DATE.getCodifier(),
                                exchange.getProperty("auditRef", String.class)));

                        exchange.getMessage().setBody(
                                DollarUUtil.buildOKDollarURequest(
                                        exchange.getProperty("dollarURessourceName", String.class),
                                        references,
                                        metadatas
                                )
                        );
                    })
                    .to("direct:dollarUCommand")

                    // Archive fichier ICE
                    .process(exchange ->
                            FileMoveUtil.move(exchange, exchange.getProperty("archivesSubdir", String.class), "inputFilePath")
                    )

                    // Audit fin fichier ICE
                    .process(exchange -> {
                        auditHelper.audit(exchange)
                                .ref(Codifier.DATE.getCodifier(), exchange.getProperty("auditRef", String.class))
                                .desc("Fichier ICE transformé et envoyé à ACADIA")
                                .status(Status.Success.getStatus())
                                .data("")
                                .meta(Codifier.PROCESS_NAME.getCodifier(), PROCESS_NAME)
                                .meta(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("inputFileName", String.class))
                                .send();
                    })

                .end()

                // Archive CSO à la fin du traitement global
                .setProperty("inputFilePath", exchangeProperty("csoInputFilePath"))
                .setProperty("inputFileName", exchangeProperty("csoInputFileName"))
                .process(exchange ->
                        FileMoveUtil.move(exchange, exchange.getProperty("archivesSubdir", String.class), "inputFilePath")
                )

                // Audit fin traitement global
                .process(exchange -> {
                    auditHelper.audit(exchange)
                            .ref(Codifier.DATE.getCodifier(), exchange.getProperty("auditRef", String.class))
                            .desc("CSO Report ACADIA traité, fichiers ICE transformés")
                            .status(Status.Success.getStatus())
                            .data("")
                            .meta(Codifier.PROCESS_NAME.getCodifier(), PROCESS_NAME)
                            .meta(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("csoInputFileName", String.class))
                            .send();
                });
    }

    private void handleException(Exchange exchange) throws Exception {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);

        errorHelper.error(exchange, exception)
                .ref(Codifier.DATE.getCodifier(), exchange.getProperty("auditRef", String.class))
                .meta(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("inputFileName", String.class))
                .meta(Codifier.PROCESS_NAME.getCodifier(), PROCESS_NAME)
                .send();

        List<Metadata> metadatas = new ArrayList<>();
        metadatas.add(new Metadata(Codifier.PROCESS_NAME.getCodifier(), PROCESS_NAME));
        metadatas.add(new Metadata(Codifier.FILE_NAME.getCodifier(),
                exchange.getProperty("inputFileName", String.class)));

        List<Reference> references = new ArrayList<>();
        references.add(new Reference(Codifier.FILE_NAME.getCodifier(),
                exchange.getProperty("inputFileName", String.class)));

        exchange.getMessage().setBody(
                DollarUUtil.buildNOKDollarURequest(
                        exchange.getProperty("dollarURessourceName", String.class),
                        references,
                        metadatas
                )
        );

        exchange.getContext()
                .createProducerTemplate()
                .send("direct:dollarUCommand", exchange);

        if (exchange.getProperty("inputFilePath") != null) {
            FileMoveUtil.move(exchange, exchange.getProperty("errorsSubdir", String.class), "inputFilePath");
        }

        log.error("PrepareFileForAcadia NOK: reason={}",
                (exception != null ? exception.getMessage() : "unknown"),
                exception);
    }
}
