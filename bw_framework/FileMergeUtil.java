import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FileMergeUtil {

    private FileMergeUtil() {
        // util class
    }

    /**
     * Merge plusieurs fichiers texte en un seul.
     *
     * @param filesToMerge   liste des fichiers source (ordre conservé)
     * @param targetFile     fichier cible
     * @param mergeHeaders   true = écrire le header une seule fois
     * @param avoidDuplicates true = supprimer les lignes dupliquées
     */
    public static void mergeFiles(
            List<Path> filesToMerge,
            Path targetFile,
            boolean mergeHeaders,
            boolean avoidDuplicates
    ) throws IOException {

        if (filesToMerge == null || filesToMerge.isEmpty()) {
            throw new IllegalArgumentException("filesToMerge is empty");
        }

        Files.createDirectories(targetFile.getParent());

        Set<String> seenLines = avoidDuplicates ? new HashSet<>() : null;
        boolean headerWritten = false;

        try (BufferedWriter writer =
                     Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8)) {

            for (Path source : filesToMerge) {

                if (source == null || !Files.exists(source)) {
                    continue; // ou throw selon ton besoin
                }

                try (BufferedReader reader =
                             Files.newBufferedReader(source, StandardCharsets.UTF_8)) {

                    String line;
                    boolean isFirstLine = true;

                    while ((line = reader.readLine()) != null) {

                        // gestion header
                        if (mergeHeaders && isFirstLine) {
                            isFirstLine = false;

                            if (headerWritten) {
                                continue; // skip header
                            } else {
                                headerWritten = true;
                            }
                        }

                        isFirstLine = false;

                        // gestion doublons
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
