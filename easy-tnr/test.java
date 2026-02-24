import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LectureRepertoire {
    
    public static void main(String[] args) {
        // Vérifier si le chemin du répertoire est fourni en argument
        if (args.length == 0) {
            System.out.println("Veuillez spécifier le chemin du répertoire en argument.");
            System.out.println("Exemple : java LectureRepertoire /chemin/vers/mon/repertoire");
            return;
        }
        
        String cheminRepertoire = args[0];
        Path repertoirePath = Paths.get(cheminRepertoire);
        
        try {
            // Vérifier si le chemin existe et est un répertoire
            if (!Files.exists(repertoirePath)) {
                System.out.println("Le chemin spécifié n'existe pas : " + cheminRepertoire);
                return;
            }
            
            if (!Files.isDirectory(repertoirePath)) {
                System.out.println("Le chemin spécifié n'est pas un répertoire : " + cheminRepertoire);
                return;
            }
            
            // Lire le contenu du répertoire et créer la Map
            Map<String, String> fichiersContenu = lireFichiersDuRepertoire(repertoirePath);
            
            // Afficher les résultats
            afficherResultats(fichiersContenu);
            
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du répertoire : " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Lit tous les fichiers du répertoire et retourne une Map avec
     * le nom du fichier en clé et son contenu en valeur
     * 
     * @param repertoirePath Le chemin du répertoire à lire
     * @return Map contenant les noms de fichiers et leur contenu
     * @throws IOException En cas d'erreur d'entrée/sortie
     */
    public static Map<String, String> lireFichiersDuRepertoire(Path repertoirePath) throws IOException {
        Map<String, String> resultat = new HashMap<>();
        
        // Utilisation de Java 8 NIO avec try-with-resources
        try (var stream = Files.list(repertoirePath)) {
            stream
                .filter(Files::isRegularFile) // Ne garder que les fichiers (pas les sous-répertoires)
                .forEach(path -> {
                    try {
                        // Lire tout le contenu du fichier
                        String contenu = Files.readString(path);
                        // Ajouter à la Map avec le nom du fichier comme clé
                        resultat.put(path.getFileName().toString(), contenu);
                    } catch (IOException e) {
                        System.err.println("Erreur lors de la lecture du fichier " + path.getFileName() + " : " + e.getMessage());
                    }
                });
        }
        
        return resultat;
    }
    
    /**
     * Affiche le contenu de la Map de façon formatée
     * 
     * @param map La Map à afficher
     */
    public static void afficherResultats(Map<String, String> map) {
        if (map.isEmpty()) {
            System.out.println("Aucun fichier trouvé dans le répertoire.");
            return;
        }
        
        System.out.println("\n=== CONTENU DES FICHIERS ===\n");
        System.out.println("Nombre de fichiers trouvés : " + map.size() + "\n");
        
        map.forEach((nomFichier, contenu) -> {
            System.out.println("Fichier : " + nomFichier);
            System.out.println("Contenu :");
            System.out.println("--------");
            System.out.println(contenu);
            System.out.println("--------\n");
        });
    }
}
