import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class NotificationMergeRoute extends RouteBuilder {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public void configure() {

        // Global error handling for this RouteBuilder
        onException(Exception.class)
            .handled(true)
            // Camel already stores the exception in Exchange.EXCEPTION_CAUGHT
            .process(this::handleException);

        fromF("file:{{notif.dir}}"
                + "?include={{notif.name}}"
                + "&delay={{poll.delay.ms}}"
                + "&noop=true"                 // keep notification until we delete it ourselves
                + "&idempotent=true"
                + "&readLock=changed")
            .routeId("notificationMergeRoute")

            // Save notification file path for cleanup
            .setProperty("notifPath", header("CamelFilePath"))

            // Cache parameters as exchange properties (easy to reuse)
            .setProperty("flowTypeBase", simple("{{flowType}}"))
            .setProperty("inputDir", simple("{{input.dir}}"))
            .setProperty("filePattern", simple("{{file.pattern}}"))
            .setProperty("outputDir", simple("{{output.dir}}"))
            .setProperty("outputPrefix", simple("{{output.prefix}}"))
            .setProperty("archivesSubdir", simple("{{archives.subdir}}"))
            .setProperty("errorsSubdir", simple("{{errors.subdir}}"))

            // 1) List matching files
            .process(this::listFilesToMerge)

            // 2) Merge files
            .process(this::mergeFiles)

            // 3) Send DollarU OK (flowType + "_OK", quantity=1)
            .process(e -> e.getMessage().setBody(buildDollarURequest(
                e.getProperty("flowTypeBase", String.class) + "_OK",
                1
            )))
            .to("direct:dollarUCommand")

            // 4) Move files to archives
            .process(e -> moveListedFiles(
                e,
                e.getProperty("archivesSubdir", String.class)
            ))

            // 5) Delete notification at the end (success)
            .process(this::deleteNotification)

            // Optional trace
            .log("Merge OK: mergedFile=${exchangeProperty.mergedFile}");
    }

    /**
     * Handles any exception raised in the route.
     * Responsibilities:
     * - send DollarU NOK (flowType + "_NOK", quantity=1)
     * - delete notification file
     * - move listed files (if any) to errors subdirectory
     */
    private void handleException(Exchange e) throws Exception {
        Exception ex = e.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);

        String base = e.getProperty("flowTypeBase", String.class);
        String nokFlowType = base + "_NOK";

        // 1) Send DollarU NOK
        e.getMessage().setBody(buildDollarURequest(nokFlowType, 1));
        e.getContext().createProducerTemplate().send("direct:dollarUCommand", e);

        // 2) Always delete notification
        deleteNotification(e);

        // 3) Move listed files to errors (if list exists)
        moveListedFiles(e, e.getProperty("errorsSubdir", String.class));

        // 4) Log the error (optional)
        log.error("Merge NOK: reason={}", (ex != null ? ex.getMessage() : "unknown"), ex);
    }

    private void listFilesToMerge(Exchange e) throws Exception {
        String inputDir = e.getProperty("inputDir", String.class);
        String regex = e.getProperty("filePattern", String.class);

        Pattern pattern = Pattern.compile(regex);

        try (Stream<Path> s = Files.list(Paths.get(inputDir))) {
            List<Path> files = s
                .filter(Files::isRegularFile)
                .filter(p -> pattern.matcher(p.getFileName().toString()).matches())
                .sorted()
                .collect(Collectors.toList());

            e.setProperty("filesToMerge", files);
        }

        List<Path> files = (List<Path>) e.getProperty("filesToMerge");
        if (files == null || files.isEmpty()) {
            throw new IllegalStateException("No files matched pattern to merge");
        }
    }

    private void mergeFiles(Exchange e) throws Exception {
        List<Path> files = (List<Path>) e.getProperty("filesToMerge");

        String outputDir = e.getProperty("outputDir", String.class);
        String prefix = e.getProperty("outputPrefix", String.class);

        String date = LocalDate.now().format(YYYYMMDD);
        Path target = Paths.get(outputDir, prefix + date + ".csv");

        FileMergeUtil.mergeFiles(
            files,
            target,
            true,   // mergeHeaders
            true    // avoidDuplicates
        );

        e.setProperty("mergedFile", target.toString());
    }

    private void moveListedFiles(Exchange e, String subdir) throws Exception {
        List<Path> files = (List<Path>) e.getProperty("filesToMerge");
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

    private DollarUCommandRequest buildDollarURequest(String flowType, int quantity) {
        DollarUCommandRequest req = new DollarUCommandRequest();
        DollarUCommandParameters p = new DollarUCommandParameters();
        p.setFlowType(flowType);
        p.setQuantity(quantity);
        req.setCommandParameters(p);
        return req;
    }
}
