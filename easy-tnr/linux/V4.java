import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class DetecteurFichiersSpeciaux {

    // On autorise : lettres, chiffres, point, underscore et tiret.
    // Tout le reste (accents, espaces, symboles) sera signalé.
    private static final Pattern PATTERN_VALIDE = Pattern.compile("^[a-zA-Z0-9._-]+$");

    public static void main(String[] args) {
        // Chemin vers votre dossier de données sur le serveur
        Path rootPath = Paths.get("/serveur_apps/easy-tnr/data/");

        if (!Files.exists(rootPath)) {
            System.err.println("Erreur : Le répertoire " + rootPath + " n'existe pas.");
            return;
        }

        System.out.println("Début de l'analyse récursive : " + rootPath.toAbsolutePath());
        System.out.println("-------------------------------------------------------");

        try {
            // Utilisation du Stream de Java 8 pour parcourir l'arborescence
            Files.walk(rootPath)
                .forEach(path -> {
                    String name = path.getFileName().toString();
                    
                    // On vérifie si le nom du fichier ou du dossier est invalide
                    if (!PATTERN_VALIDE.matcher(name).matches()) {
                        System.out.println("[ALERTE] Caractère spécial ou accent détecté :");
                        System.out.println("  Nom : " + name);
                        System.out.println("  Lien : " + path.toAbsolutePath());
                        System.out.println();
                    }
                });
        } catch (IOException e) {
            System.err.println("Erreur lors du parcours : " + e.getMessage());
        }

        System.out.println("-------------------------------------------------------");
        System.out.println("Analyse terminée.");
    }
}
 
