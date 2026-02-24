import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class FileMatcher {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java FileMatcher <dir_xml> <dir_recherche>");
            return;
        }

        Path dirXml = Paths.get(args[0]);
        Path dirSearch = Paths.get(args[1]);

        try {
            // 1. Charger les messages du premier répertoire (Set pour éviter les doublons)
            System.out.println("Analyse du répertoire source : " + dirXml);
            Set<String> messageContents = loadMessagesFromDir(dirXml);
            System.out.println("Nombre de messages uniques extraits : " + messageContents.size());

            // 2. Charger le répertoire cible dans une Map <NomFichier, Contenu>
            System.out.println("Chargement de la Map de recherche : " + dirSearch);
            Map<String, String> fileMap = loadFilesToMap(dirSearch);
            System.out.println("Nombre de fichiers cibles chargés : " + fileMap.size());

            // 3. Comparaison et Affichage des correspondances
            System.out.println("\n=== RÉSULTATS DE LA RECHERCHE ===");
            
            for (String message : messageContents) {
                List<String> matchingFiles = new ArrayList<>();
                
                // On cherche dans chaque fichier de la Map
                for (Map.Entry<String, String> entry : fileMap.entrySet()) {
                    if (entry.getValue().contains(message)) {
                        matchingFiles.add(entry.getKey());
                    }
                }

                // Affichage propre par message
                String preview = message.length() > 50 ? message.substring(0, 50).replace("\n", " ") + "..." : message;
                System.out.println("\nMessage : [" + preview + "]");
                
                if (matchingFiles.isEmpty()) {
                    System.out.println("  -> AUCUNE CORRESPONDANCE TROUVÉE");
                } else {
                    System.out.println("  -> TROUVÉ DANS : " + String.join(", ", matchingFiles));
                }
            }

        } catch (IOException e) {
            System.err.println("Erreur lors du traitement des fichiers : " + e.getMessage());
        }
    }

    private static Set<String> loadMessagesFromDir(Path dir) throws IOException {
        // Regex gérant les espaces potentiels et le contenu multi-ligne
        Pattern pattern = Pattern.compile("<ns0:messageContent[^>]*>(.*?)</ns0:messageContent>", Pattern.DOTALL);
        
        return Files.walk(dir)
                .filter(Files::isRegularFile)
                .map(path -> {
                    try {
                        String content = new String(Files.readAllBytes(path));
                        Matcher matcher = pattern.matcher(content);
                        if (matcher.find()) {
                            return matcher.group(1).trim(); // On nettoie le contenu extrait
                        }
                    } catch (IOException e) {
                        System.err.println("Erreur lecture fichier source : " + path);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static Map<String, String> loadFilesToMap(Path dir) throws IOException {
        return Files.walk(dir)
                .filter(Files::isRegularFile)
                .collect(Collectors.toMap(
                        path -> path.getFileName().toString(),
                        path -> {
                            try {
                                return new String(Files.readAllBytes(path));
                            } catch (IOException e) {
                                return "";
                            }
                        },
                        (existing, replacement) -> existing // Garde le premier en cas de doublon de nom
                ));
    }
}
