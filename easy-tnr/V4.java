import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RechercheMessageContent {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java RechercheMessageContent <chemin_repertoire>");
            return;
        }
        
        Path repertoire = Paths.get(args[0]);
        
        try {
            // 1. Charger tous les fichiers et leur contenu dans une Map
            Map<String, String> mapFichiers = chargerFichiers(repertoire);
            System.out.println("Fichiers chargés: " + mapFichiers.size());
            
            // 2. Extraire le contenu entre les balises messageContent dans un Set
            Set<String> contenusMessages = extraireContenuMessages(mapFichiers);
            System.out.println("Contenus messageContent extraits: " + contenusMessages.size());
            
            // 3. Pour chaque contenu, chercher les 100 premiers caractères dans la map
            Map<String, List<String>> resultatsRecherche = new HashMap<>();
            
            int compteur = 1;
            for (String contenu : contenusMessages) {
                // Prendre les 100 premiers caractères (ou moins si le contenu est plus court)
                String debutContenu = contenu.length() > 100 ? contenu.substring(0, 100) : contenu;
                
                System.out.println("\n--- Recherche pour message " + compteur + " ---");
                System.out.println("Début du contenu (" + debutContenu.length() + " caractères):");
                System.out.println(debutContenu + "...");
                
                // Rechercher ce début de contenu dans tous les fichiers
                List<String> fichiersTrouves = new ArrayList<>();
                
                for (Map.Entry<String, String> entry : mapFichiers.entrySet()) {
                    String nomFichier = entry.getKey();
                    String contenuFichier = entry.getValue();
                    
                    // Vérifier si le début du contenu apparaît dans le fichier
                    if (contenuFichier.contains(debutContenu)) {
                        fichiersTrouves.add(nomFichier);
                    }
                }
                
                resultatsRecherche.put("Message_" + compteur, fichiersTrouves);
                
                // Afficher les résultats
                if (fichiersTrouves.isEmpty()) {
                    System.out.println("  → Aucun fichier trouvé contenant ce début de message");
                } else {
                    System.out.println("  → Trouvé dans " + fichiersTrouves.size() + " fichier(s):");
                    for (String fichier : fichiersTrouves) {
                        System.out.println("    - " + fichier);
                    }
                }
                
                compteur++;
            }
            
            // 4. Afficher un résumé final
            afficherResume(resultatsRecherche);
            
        } catch (IOException e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Charge tous les fichiers du répertoire dans une Map
     */
    private static Map<String, String> chargerFichiers(Path repertoire) throws IOException {
        Map<String, String> mapFichiers = new HashMap<>();
        
        System.out.println("Lecture du répertoire: " + repertoire);
        
        Files.list(repertoire)
            .filter(Files::isRegularFile)
            .forEach(path -> {
                try {
                    String nomFichier = path.getFileName().toString();
                    String contenu = new String(Files.readAllBytes(path));
                    mapFichiers.put(nomFichier, contenu);
                    System.out.println("  → Chargé: " + nomFichier + " (" + contenu.length() + " caractères)");
                } catch (IOException e) {
                    System.err.println("Erreur lecture: " + path.getFileName() + " - " + e.getMessage());
                }
            });
        
        return mapFichiers;
    }
    
    /**
     * Extrait le contenu entre les balises <ns0:messageContent> et </ns0:messageContent>
     */
    private static Set<String> extraireContenuMessages(Map<String, String> mapFichiers) {
        Set<String> contenusMessages = new HashSet<>();
        
        // Pattern pour capturer le contenu entre les balises avec l'attribut xmlns
        // On utilise une regex avec groupe de capture
        Pattern pattern = Pattern.compile(
            "<ns0:messageContent\\s+xmlns:ns0=\"http://www\\.lbp\\.fr/xml\">(.*?)</ns0:messageContent>",
            Pattern.DOTALL  // Pour que .* capture aussi les retours à la ligne
        );
        
        System.out.println("\nExtraction du contenu des balises messageContent...");
        
        for (Map.Entry<String, String> entry : mapFichiers.entrySet()) {
            String nomFichier = entry.getKey();
            String contenu = entry.getValue();
            
            Matcher matcher = pattern.matcher(contenu);
            int compteur = 0;
            
            while (matcher.find()) {
                String contenuMessage = matcher.group(1).trim();
                if (!contenuMessage.isEmpty()) {
                    contenusMessages.add(contenuMessage);
                    compteur++;
                }
            }
            
            if (compteur > 0) {
                System.out.println("  → " + nomFichier + ": " + compteur + " message(s) extrait(s)");
            }
        }
        
        return contenusMessages;
    }
    
    /**
     * Version alternative: Extraction avec gestion des balises sans attribut
     */
    private static Set<String> extraireContenuMessagesSimple(Map<String, String> mapFichiers) {
        Set<String> contenusMessages = new HashSet<>();
        
        String baliseOuvrante = "<ns0:messageContent xmlns:ns0=\"http://www.lbp.fr/xml\">";
        String baliseFermante = "</ns0:messageContent>";
        
        for (Map.Entry<String, String> entry : mapFichiers.entrySet()) {
            String contenu = entry.getValue();
            int indexDebut = 0;
            
            while (true) {
                indexDebut = contenu.indexOf(baliseOuvrante, indexDebut);
                if (indexDebut == -1) break;
                
                indexDebut += baliseOuvrante.length();
                int indexFin = contenu.indexOf(baliseFermante, indexDebut);
                
                if (indexFin != -1) {
                    String contenuMessage = contenu.substring(indexDebut, indexFin).trim();
                    if (!contenuMessage.isEmpty()) {
                        contenusMessages.add(contenuMessage);
                    }
                    indexDebut = indexFin + baliseFermante.length();
                } else {
                    break;
                }
            }
        }
        
        return contenusMessages;
    }
    
    /**
     * Affiche un résumé des résultats de recherche
     */
    private static void afficherResume(Map<String, List<String>> resultats) {
        System.out.println("\n=== RÉSUMÉ FINAL ===");
        System.out.println("Total messages traités: " + resultats.size());
        
        int totalFichiersTrouves = resultats.values().stream()
            .mapToInt(List::size)
            .sum();
        
        System.out.println("Total correspondances trouvées: " + totalFichiersTrouves);
        
        // Afficher les messages sans correspondance
        List<String> sansCorrespondance = resultats.entrySet().stream()
            .filter(e -> e.getValue().isEmpty())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        if (!sansCorrespondance.isEmpty()) {
            System.out.println("\nMessages sans fichier correspondant: " + sansCorrespondance.size());
            sansCorrespondance.forEach(msg -> System.out.println("  → " + msg));
        }
    }
}
