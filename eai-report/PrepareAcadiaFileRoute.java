package fr.labanquepostale.report.base.routes.acadia;

import fr.labanquepostale.marches.eai.core.helper.AuditHelper;
import fr.labanquepostale.marches.eai.core.helper.ErrorHelper;
import fr.labanquepostale.marches.eai.core.io.FileListingUtil;
import fr.labanquepostale.marches.eai.core.io.FileMoveUtil;
import fr.labanquepostale.marches.eai.core.model.audit.Codifier;
import fr.labanquepostale.marches.eai.core.model.audit.Status;
import fr.labanquepostale.report.base.beans.acadia.AcadiaBean;
import fr.labanquepostale.report.base.model.acadia.IceLine;
import fr.labanquepostale.report.base.model.acadia.CalypsoLine;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Component
public class PrepareAcadiaFileRoute extends RouteBuilder {

    private static final String PROCESS_NAME = PrepareAcadiaFileRoute.class.getName();

    private static final String OUTPUT_HEADER =
            "ValuationDate\tPortfolioID\tTradeID\tEndDate\tIMModel\tPostRegulations\tCollectRegulations\tProductClass\tSensitivity_Id\tRiskType\tQualifier\tBucket\tLabel1\tLabel2\tAmount\tAmountCurrency\tAmountUSD";

    @Autowired
    private AuditHelper auditHelper;

    @Autowired
    private ErrorHelper errorHelper;

    private final AcadiaBean bean;

    public PrepareAcadiaFileRoute(AcadiaBean bean) {
        this.bean = bean;
    }

    @Override
    public void configure() {

        onException(Exception.class)
                .handled(true)
                .process(this::handleException);

        BindyCsvDataFormat calypsoCSV = new BindyCsvDataFormat(CalypsoLine.class);
        BindyCsvDataFormat iceCSV = new BindyCsvDataFormat(IceLine.class);

        fromF("file:{{eai.report.acadia.prepare.input.cso.dir}}"
                + "?include={{eai.report.acadia.prepare.input.cso.file.pattern}}"
                + "&delay={{eai.report.acadia.prepare.input.delay.ms}}"
                + "&noop=true"
                + "&idempotent=false"
                + "&readLock=changed")
                .routeId("PrepareFileForAcadia")

                // contexte global
                .setProperty("auditRef", simple("${date:now:ddMMyyyyHHmmss}"))
                .setProperty("inputIceDir", simple("{{eai.report.acadia.prepare.input.ice.dir}}"))
                .setProperty("icePattern", simple("{{eai.report.acadia.prepare.input.ice.file.pattern}}"))
                .setProperty("archivesSubdir", simple("{{eai.report.acadia.prepare.input.dir.archives.subdir}}"))
                .setProperty("errorsSubdir", simple("{{eai.report.acadia.prepare.input.dir.errors.subdir}}"))
                .setProperty("outputDir", simple("{{eai.report.acadia.prepare.output.dir}}"))
                .setProperty("iceEncoding", simple("{{eai.report.acadia.prepare.input.ice.encoding}}"))
                .setProperty("outputExtension", simple("{{eai.report.acadia.prepare.output.file.extension}}"))

                // fichier CSO courant
                .setProperty("csoInputFilePath", header("CamelFilePath"))
                .setProperty("csoInputFileName", header("CamelFileName"))

                // audit début traitement CSO
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

                // parse CSO puis construction du mapping TradeID -> ExternalTradeID
                .unmarshal(calypsoCSV)
                .bean(bean, "buildTradeIdMap")
                .setProperty("tradeIdMapping", body())

                // listing des fichiers ICE
                .process(exchange -> FileListingUtil.listFiles(
                        exchange,
                        Paths.get(exchange.getProperty("inputIceDir", String.class)),
                        false,
                        FileListingUtil.SortCriterion.NAME,
                        FileListingUtil.SortDirection.DESC,
                        exchange.getProperty("icePattern", String.class)
                ))

                .setBody(exchangeProperty("inputFiles"))
                .split(body()).stopOnException()

                .process(exchange -> {
                    Path inputFile = exchange.getIn().getBody(Path.class);
                    if (inputFile == null) {
                        throw new IllegalStateException("Aucun fichier ICE trouvé dans le split.");
                    }
                    String inputFileName = inputFile.getFileName().toString();
                    String csoFileName = exchange.getProperty("csoInputFileName", String.class);
                    exchange.setProperty("inputFilePath", inputFile.toString());
                    exchange.setProperty("inputFileName", inputFileName);
                })

                // lecture brute du fichier ICE
                .process(exchange -> {
                    String raw = bean.readFile(
                            exchange.getProperty("inputFilePath", String.class),
                            exchange.getProperty("iceEncoding", String.class)
                    );
                    exchange.getIn().setBody(raw);
                })

                // audit début traitement ICE
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

                // parse ICE
                .unmarshal(iceCSV)

                // transformation métier
                .process(exchange -> {
                    @SuppressWarnings("unchecked")
                    List<IceLine> lines = exchange.getIn().getBody(List.class);

                    @SuppressWarnings("unchecked")
                    Map<String, String> tradeIdMapping = exchange.getProperty("tradeIdMapping", Map.class);
                    String inputFileName = exchange.getProperty("inputFileName", String.class);
                    List<IceLine> transformed = bean.transformIceLines(lines, tradeIdMapping, bean.extractPortfolioIdFromFileName(inputFileName));
                    exchange.getIn().setBody(transformed);
                })

                // rendu final via Bindy
                .marshal(iceCSV)
                .process(exchange -> {
                    String marshalledBody = exchange.getIn().getBody(String.class);
                    String iceFileContent = OUTPUT_HEADER + "\n" + marshalledBody;
                    exchange.getIn().setBody(iceFileContent);
                    exchange.setProperty("iceFileContent",iceFileContent);
                })

                // nom du fichier de sortie
                .setProperty("outputFileName", method(bean,
                        "buildOutputFileName(${exchangeProperty.inputFileName}, ${exchangeProperty.outputExtension})"))

                // écriture du fichier produit
                .toD("file:${exchangeProperty.outputDir}?fileName=${exchangeProperty.outputFileName}")

                // archive fichier ICE
                .process(exchange ->
                        FileMoveUtil.move(
                                exchange,
                                exchange.getProperty("archivesSubdir", String.class),
                                "inputFilePath"
                        )
                )

                // audit fin traitement du fichier ICE
                .process(exchange -> {
                    auditHelper.audit(exchange)
                            .ref(Codifier.DATE.getCodifier(), exchange.getProperty("auditRef", String.class))
                            .desc("Fichier ICE pour ACADIA traité")
                            .status(Status.Success.getStatus())
                            .data(exchange.getProperty("iceFileContent",String.class))
                            .meta(Codifier.PROCESS_NAME.getCodifier(), PROCESS_NAME)
                            .meta(Codifier.FILE_NAME.getCodifier(), exchange.getProperty("inputFileName", String.class))
                            .send();
                })

                .end()

                // archive CSO à la fin du traitement global
                .setProperty("inputFilePath", exchangeProperty("csoInputFilePath"))
                .setProperty("inputFileName", exchangeProperty("csoInputFileName"))
                .process(exchange ->
                        FileMoveUtil.move(
                                exchange,
                                exchange.getProperty("archivesSubdir", String.class),
                                "inputFilePath"
                        )
                )

                // audit fin global
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

        if (exchange.getProperty("inputFilePath") != null) {
            FileMoveUtil.move(exchange, exchange.getProperty("errorsSubdir", String.class), "inputFilePath");
        }

        log.error("PrepareFileForAcadia NOK: reason={}",
                exception != null ? exception.getMessage() : "unknown",
                exception);
    }
}
