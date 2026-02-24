import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LectureRepertoire {
    
    public static void main(String[] args) {
        // Vérifier si le chemin du répertoire est fourni
        if (args.length == 0) {
            System.out.println("Veuillez spécifier le chemin du répertoire.");
            System.out.println("Usage: java LectureRepertoire /chemin/vers/repertoire");
            return;
        }
        
        String cheminRepertoire = args[0];
        Path repertoirePath = Paths.get(cheminRepertoire);
        
        try {
            // Vérifications de base
            if (!Files.exists(repertoirePath) || !Files.isDirectory(repertoirePath)) {
                System.out.println("Le chemin n'est pas valide : " + cheminRepertoire);
                return;
            }
            
            // 1. Lire les fichiers et créer la Map
            Map<String, String> fichiersContenu = new HashMap<>();
            
            Files.list(repertoirePath)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        String contenu = new String(Files.readAllBytes(path));
                        fichiersContenu.put(path.getFileName().toString(), contenu);
                    } catch (IOException e) {
                        System.err.println("Erreur lecture: " + path.getFileName());
                    }
                });
            
            System.out.println("Fichiers chargés: " + fichiersContenu.size());
            
            // 2. Liste de mots à rechercher
            List<String> motsRecherche = new ArrayList<>();
            motsRecherche.add("Java");
            motsRecherche.add("test");
            motsRecherche.add("important");
            
            // 3. Recherche simple
            for (String mot : motsRecherche) {
                System.out.println("\nRecherche de: \"" + mot + "\"");
                boolean trouve = false;
                
                for (Map.Entry<String, String> entry : fichiersContenu.entrySet()) {
                    if (entry.getValue().contains(mot)) {
                        System.out.println("  → Trouvé dans: " + entry.getKey());
                        trouve = true;
                    }
                }
                
                if (!trouve) {
                    System.out.println("  → Aucun fichier trouvé");
                }
            }
            
        } catch (IOException e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }
}
