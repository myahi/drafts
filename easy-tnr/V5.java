import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class FileMatcher {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java FileMatcher <dir_xml> <dir_map>");
            return;
        }

        Path dirXml = Paths.get(args[0]);
        Path dirMap = Paths.get(args[1]);

        try {
            // 1. Charger le contenu des balises dans un Set
            Set<String> messageContents = loadMessagesFromDir(dirXml);
            System.out.println("Messages extraits : " + messageContents.size());

            // 2. Charger le deuxième répertoire dans une Map <NomFichier, Contenu>
            Map<String, String> fileMap = loadFilesToMap(dirMap);
            System.out.println("Fichiers chargés dans la map : " + fileMap.size());

            // 3. Chercher chaque message dans la Map
            System.out.println("\n--- Résultats de la recherche ---");
            messageContents.forEach(message -> {
                boolean found = false;
                for (Map.Entry<String, String> entry : fileMap.entrySet()) {
                    if (entry.getValue().contains(message)) {
                        System.out.println("Message trouvé dans le fichier : " + entry.getKey());
                        found = true;
                    }
                }
                if (!found) {
                    System.out.println("Message non trouvé pour : " + 
                        (message.length() > 30 ? message.substring(0, 30) + "..." : message));
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
                        System.err.println("Erreur lecture : " + path);
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
                        (existing, replacement) -> existing // En cas de doublons de noms
                ));
    }
}
