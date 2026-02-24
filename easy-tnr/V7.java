import java.io.IOException;
import java.io.BufferedReader;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class FileMatcher {

    // Constante pour limiter la lecture des fichiers cibles
    private static final int MAX_CHARS = 100;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java FileMatcher <dir_xml_source> <dir_recherche_cible>");
            return;
        }

        Path dirXml = Paths.get(args[0]);
        Path dirSearch = Paths.get(args[1]);

        try {
            // 1. Extraire les messages du premier répertoire (Set)
            System.out.println("1. Analyse des balises dans : " + dirXml);
            Set<String> messageContents = loadMessagesFromDir(dirXml);
            System.out.println("   -> Messages uniques extraits : " + messageContents.size());

            // 2. Charger les 100 premiers caractères des fichiers cibles (Map)
            System.out.println("2. Lecture des " + MAX_CHARS + " premiers caractères dans : " + dirSearch);
            Map<String, String> fileMap = loadFilesHeadToMap(dirSearch);
            System.out.println("   -> Fichiers indexés : " + fileMap.size());

            // 3. Comparaison et affichage
            System.out.println("\n=== RÉSULTATS DE LA RECHERCHE (Sur les " + MAX_CHARS + " ers caractères) ===");
            
            if (messageContents.isEmpty()) {
                System.out.println("Aucun message n'a été extrait des fichiers sources.");
                return;
            }

            for (String message : messageContents) {
                List<String> matchingFiles = new ArrayList<>();
                
                // On cherche si le message est contenu dans l'extrait de 100 caractères
                for (Map.Entry<String, String> entry : fileMap.entrySet()) {
                    if (entry.getValue().contains(message)) {
                        matchingFiles.add(entry.getKey());
                    }
                }

                // Affichage du résultat pour ce message
                String preview = message.length() > 40 ? message.substring(0, 40) + "..." : message;
                System.out.println("\nMessage cherché : [" + preview.replace("\n", " ") + "]");
                
                if (matchingFiles.isEmpty()) {
                    System.out.println("   [!] AUCUNE CORRESPONDANCE");
                } else {
                    System.out.println("   [+] TROUVÉ DANS : " + String.join(", ", matchingFiles));
                }
            }

        } catch (IOException e) {
            System.err.println("Erreur fatale : " + e.getMessage());
        }
    }

    /**
     * Extrait le contenu des balises ns0:messageContent dans le répertoire source
     */
    private static Set<String> loadMessagesFromDir(Path dir) throws IOException {
        Pattern pattern = Pattern.compile("<ns0:messageContent[^>]*>(.*?)</ns0:messageContent>", Pattern.DOTALL);
        
        return Files.walk(dir)
                .filter(Files::isRegularFile)
                .map(path -> {
                    try {
                        String content = new String(Files.readAllBytes(path));
                        Matcher matcher = pattern.matcher(content);
                        if (matcher.find()) {
                            return matcher.group(1).trim();
                        }
                    } catch (IOException e) {
                        System.err.println("Erreur lecture source : " + path.getFileName());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Charge uniquement le début (100 caractères) de chaque fichier du répertoire cible
     */
    private static Map<String, String> loadFilesHeadToMap(Path dir) throws IOException {
        return Files.walk(dir)
                .filter(Files::isRegularFile)
                .collect(Collectors.toMap(
                        path -> path.getFileName().toString(),
                        path -> {
                            try (BufferedReader reader = Files.newBufferedReader(path)) {
                                char[] buffer = new char[MAX_CHARS];
                                int lus = reader.read(buffer, 0, MAX_CHARS);
                                return (lus == -1) ? "" : new String(buffer, 0, lus);
                            } catch (IOException e) {
                                return ""; // Fichier illisible ou binaire
                            }
                        },
                        (existing, replacement) -> existing
                ));
    }
}
