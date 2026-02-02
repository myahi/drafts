import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class used to merge multiple text files into a single target file.
 *
 * <p>Typical use cases:
 * <ul>
 *   <li>Merging CSV files</li>
 *   <li>Merging flat text files</li>
 *   <li>Batch or Camel-based file processing</li>
 * </ul>
 *
 * <p>Features:
 * <ul>
 *   <li>Optional header merging (header written once)</li>
 *   <li>Optional duplicate line removal</li>
 *   <li>Preserves input file order</li>
 * </ul>
 */
public final class FileMergeUtil {

    private FileMergeUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Merges multiple text files into a single target file.
     *
     * @param filesToMerge     list of source files (order is preserved)
     * @param targetFile       target file path
     * @param mergeHeaders     true to write the header only once
     * @param avoidDuplicates  true to remove duplicate lines
     * @throws IOException if an I/O error occurs
     */
    public static void mergeFiles(
            List<Path> filesToMerge,
            Path targetFile,
            boolean mergeHeaders,
            boolean avoidDuplicates
    ) throws IOException {

        if (filesToMerge == null || filesToMerge.isEmpty()) {
            throw new IllegalArgumentException("filesToMerge must not be empty");
        }

        // Ensure target directory exists
        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }

        Set<String> seenLines = avoidDuplicates ? new HashSet<>() : null;
        boolean headerWritten = false;

        try (BufferedWriter writer =
                     Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8)) {

            for (Path source : filesToMerge) {

                if (source == null || !Files.exists(source)) {
                    continue; // skip missing files
                }

                try (BufferedReader reader =
                             Files.newBufferedReader(source, StandardCharsets.UTF_8)) {

                    String line;
                    boolean isFirstLine = true;

                    while ((line = reader.readLine()) != null) {

                        // Header handling
                        if (mergeHeaders && isFirstLine) {
                            isFirstLine = false;

                            if (headerWritten) {
                                continue; // skip header line
                            } else {
                                headerWritten = true;
                            }
                        }

                        isFirstLine = false;

                        // Duplicate handling
                        if (avoidDuplicates) {
                            if (!seenLines.add(line)) {
                                continue;
                            }
                        }

                        writer.write(line);
                        writer.newLine();
                    }
                }
            }
        }
    }
}
