import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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
            
            // Afficher les résultats de la lecture
            afficherResultats(fichiersContenu);
            
            // Liste de chaînes à rechercher
            List<String> rechercheListe = Arrays.asList(
                "Hello",           // Recherche simple
                "Java",            // Recherche partielle
                "Texte inexistant" // Recherche sans résultat
            );
            
            // Effectuer la recherche
            effectuerRecherche(fichiersContenu, rechercheListe);
            
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
     * Effectue une recherche dans les valeurs de la Map pour chaque élément de la liste
     * et affiche les noms de fichiers correspondants
     * 
     * @param map La Map contenant les noms de fichiers et leur contenu
     * @param rechercheListe La liste des chaînes à rechercher
     */
    public static void effectuerRecherche(Map<String, String> map, List<String> rechercheListe) {
        System.out.println("\n=== RECHERCHE DANS LES FICHIERS ===\n");
        
        // Version 1 : Recherche exacte (le contenu doit correspondre exactement à la chaîne recherchée)
        System.out.println("--- Recherche exacte ---");
        rechercheListe.forEach(recherche -> {
            List<String> fichiersTrouves = new ArrayList<>();
            
            map.forEach((nomFichier, contenu) -> {
                if (contenu.contains(recherche)) {
                    fichiersTrouves.add(nomFichier);
                }
            });
            
            afficherResultatRecherche(recherche, fichiersTrouves);
        });
        
        // Version 2 : Recherche insensible à la casse
        System.out.println("\n--- Recherche insensible à la casse ---");
        rechercheListe.forEach(recherche -> {
            String rechercheLower = recherche.toLowerCase();
            List<String> fichiersTrouves = new ArrayList<>();
            
            map.forEach((nomFichier, contenu) -> {
                if (contenu.toLowerCase().contains(rechercheLower)) {
                    fichiersTrouves.add(nomFichier);
                }
            });
            
            afficherResultatRecherche(recherche, fichiersTrouves);
        });
        
        // Version 3 : Utilisation des Streams Java 8 pour une approche plus fonctionnelle
        System.out.println("\n--- Recherche avec Streams (insensible à la casse) ---");
        rechercheListe.forEach(recherche -> {
            String rechercheLower = recherche.toLowerCase();
            
            List<String> fichiersTrouves = map.entrySet().stream()
                .filter(entry -> entry.getValue().toLowerCase().contains(rechercheLower))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            afficherResultatRecherche(recherche, fichiersTrouves);
        });
    }
    
    /**
     * Affiche le résultat d'une recherche
     * 
     * @param recherche La chaîne recherchée
     * @param fichiersTrouves La liste des fichiers où la chaîne a été trouvée
     */
    private static void afficherResultatRecherche(String recherche, List<String> fichiersTrouves) {
        System.out.println("\nRecherche de : \"" + recherche + "\"");
        
        if (fichiersTrouves.isEmpty()) {
            System.out.println("  → Aucun fichier trouvé");
        } else {
            System.out.println("  → Trouvé dans " + fichiersTrouves.size() + " fichier(s) :");
            fichiersTrouves.forEach(nomFichier -> System.out.println("    - " + nomFichier));
            
            // Optionnel : afficher le contexte (les lignes où le texte a été trouvé)
            System.out.println("  → Contexte de la recherche :");
            fichiersTrouves.forEach(nomFichier -> {
                // Cette partie nécessiterait de relire le fichier ou de garder le contenu en mémoire
                // Pour l'exemple, on simule juste l'affichage
                System.out.println("    Dans " + nomFichier + " : le texte a été trouvé");
            });
        }
    }
    
    /**
     * Version alternative : Recherche avec retour d'une Map des résultats
     * Pour chaque chaîne recherchée, on retourne la liste des fichiers où elle a été trouvée
     * 
     * @param map La Map contenant les noms de fichiers et leur contenu
     * @param rechercheListe La liste des chaînes à rechercher
     * @return Map avec pour chaque chaîne recherchée, la liste des fichiers correspondants
     */
    public static Map<String, List<String>> rechercherAvecResultats(Map<String, String> map, List<String> rechercheListe) {
        Map<String, List<String>> resultats = new HashMap<>();
        
        rechercheListe.forEach(recherche -> {
            String rechercheLower = recherche.toLowerCase();
            
            List<String> fichiersTrouves = map.entrySet().stream()
                .filter(entry -> entry.getValue().toLowerCase().contains(rechercheLower))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            resultats.put(recherche, fichiersTrouves);
        });
        
        return resultats;
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
            System.out.println(contenu.length() > 200 ? 
                contenu.substring(0, 200) + "..." : contenu);
            System.out.println("--------\n");
        });
    }
}
