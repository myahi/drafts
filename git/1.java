import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class GitkeepGenerator {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java GitkeepGenerator <chemin_racine>");
            return;
        }

        Path rootPath = Paths.get(args[0]);

        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            System.err.println("Erreur: Le chemin spécifié n'existe pas ou n'est pas un répertoire.");
            return;
        }

        try {
            System.out.println("Début du parcours récursif à partir de : " + rootPath.toAbsolutePath());
            
            // Files.walkFileTree parcourt récursivement toute l'arborescence
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    // Vérifie si le nom du répertoire est exactement "CURRENT_RESULT"
                    if (dir.getFileName() != null && dir.getFileName().toString().equals("CURRENT_RESULT")) {
                        createGitkeep(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            System.out.println("Terminé avec succès.");

        } catch (IOException e) {
            System.err.println("Erreur lors du parcours : " + e.getMessage());
        }
    }

    private static void createGitkeep(Path directory) {
        Path gitkeepFile = directory.resolve(".gitkeep");
        try {
            // Files.write avec l'option CREATE et TRUNCATE_EXISTING (écrase si existe)
            // On écrit un tableau d'octets vide
            Files.write(gitkeepFile, new byte[0], 
                        StandardOpenOption.CREATE, 
                        StandardOpenOption.TRUNCATE_EXISTING);
            
            System.out.println("[OK] .gitkeep créé/mis à jour dans : " + directory);
        } catch (IOException e) {
            System.err.println("[ERREUR] Impossible de créer dans " + directory + " : " + e.getMessage());
        }
    }
}
