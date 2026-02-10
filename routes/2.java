
package fr.labanquepostale.report.base.utils;

import org.apache.camel.Exchange;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility to list files with sorting + optional filename filter (Linux-like glob),
 * and store results into Camel Exchange properties:
 * - inputFiles: List<Path>
 * - inputFileNames: List<String>
 */
public final class FileListingUtil {

    private FileListingUtil() {}

    public enum SortCriterion {
        NAME,
        LAST_MODIFIED_TIME
    }

    public enum SortDirection {
        ASC,
        DESC
    }

    private static Comparator<Path> comparator(SortCriterion sort) {
        if (sort == null) {
            sort = SortCriterion.NAME;
        }

        switch (sort) {
            case LAST_MODIFIED_TIME:
                return Comparator.comparing(FileListingUtil::safeLastModifiedTime);
            case NAME:
            default:
                return Comparator.comparing(
                        p -> p.getFileName().toString(),
                        String.CASE_INSENSITIVE_ORDER
                );
        }
    }

    private static FileTime safeLastModifiedTime(Path p) {
        try {
            return Files.getLastModifiedTime(p);
        } catch (IOException e) {
            // fallback: if we can't read lastModifiedTime, keep a stable ordering
            return FileTime.fromMillis(0L);
        }
    }

    /**
     * @param exchange        Camel exchange (can be null). If not null, sets:
     *                        - exchangeProperty("inputFiles")     : List<Path>
     *                        - exchangeProperty("inputFileNames"): List<String>
     * @param dir             directory to scan
     * @param recursive       if true uses Files.walk, else Files.list
     * @param sort            sorting criterion
     * @param direction       sorting direction
     * @param filenamePattern Linux-like glob (e.g. "A_*.txt", "Test_*_20260213.csv").
     *                        If null/blank => no filter.
     */
    public static List<Path> listFiles(
            Exchange exchange,
            Path dir,
            boolean recursive,
            SortCriterion sort,
            SortDirection direction,
            String filenamePattern
    ) throws IOException {

        // Prepare filename matcher (outside try, lambda-safe)
        PathMatcher matcher = null;
        if (filenamePattern != null && !filenamePattern.isBlank()) {
            try {
                matcher = FileSystems.getDefault()
                        .getPathMatcher("glob:" + filenamePattern.trim());
            } catch (PatternSyntaxException | IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Invalid filename glob pattern: '" + filenamePattern + "'. " +
                        "Example: A_*.txt or Test_*_20260213.csv",
                        ex
                );
            }
        }

        try (Stream<Path> s = recursive ? Files.walk(dir) : Files.list(dir)) {

            Comparator<Path> cmp = comparator(sort);
            if (direction == SortDirection.DESC) {
                cmp = cmp.reversed();
            }

            List<Path> files = s
                    .filter(Files::isRegularFile)
                    .filter(p -> matcher == null || matcher.matches(p.getFileName()))
                    .sorted(cmp)
                    .collect(Collectors.toList());

            // Store into Camel Exchange properties
            if (exchange != null) {
                exchange.setProperty("inputFiles", files);

                List<String> filenames = files.stream()
                        .map(p -> p.getFileName().toString())
                        .collect(Collectors.toList());

                exchange.setProperty("inputFileNames", filenames);
            }

            return files;
        }
    }
}
