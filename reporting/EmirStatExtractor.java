import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Java 8 - Extraction streaming de records <Stat> vers CSV.
 *
 * - Le XML peut être volumineux (plusieurs Mo)
 * - Les records sont des blocs répétés <Stat>...</Stat>
 * - Les colonnes sont définies par une map (nomColonne -> path relatif à Stat)
 *
 * Exemple de path: "CtrPtySpcfcData/CtrPty/RptgCtrPty/Id/Lgl/Id/LEI"
 */
public class StatXmlToCsvExtractor {

    /**
     * Définition des colonnes (ordre conservé grâce à LinkedHashMap).
     * IMPORTANT: adapte cette map à tes champs cibles.
     *
     * Ici je mets les 3 champs montrés dans ton exemple.
     */
    private static final LinkedHashMap<String, String> COLUMN_TO_PATH = new LinkedHashMap<String, String>();
    static {
        COLUMN_TO_PATH.put("ID", "CmonTradData/TxData/TxId/Prtry/Id");
        COLUMN_TO_PATH.put("LEI_REPORTING_CTP", "CtrPtySpcfcData/CtrPty/RptgCtrPty/Id/Lgl/Id/LEI");
        COLUMN_TO_PATH.put("LEI_OTHER_CTP", "CtrPtySpcfcData/CtrPty/OthrCtrPty/IdTp/Lgl/Id/LEI");
    }

    /**
     * Parse le XML en streaming et génère un CSV.
     */
    public void extract(Path xmlInput, Path csvOutput) throws Exception {
        // Précompilation: colonne -> liste de segments du path
        final List<String> columns = new ArrayList<String>(COLUMN_TO_PATH.keySet());
        final Map<String, List<String>> colPathSegments = new HashMap<String, List<String>>();
        for (Map.Entry<String, String> e : COLUMN_TO_PATH.entrySet()) {
            colPathSegments.put(e.getKey(), splitPath(e.getValue()));
        }

        XMLInputFactory factory = XMLInputFactory.newFactory();
        // Coalescing: regroupe les événements CHARACTERS contigus
        try {
            factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        } catch (IllegalArgumentException ignore) {
            // certains impl ne supportent pas, on ignore
        }

        try (InputStream in = Files.newInputStream(xmlInput);
             BufferedWriter writer = Files.newBufferedWriter(
                     csvOutput,
                     StandardCharsets.UTF_8,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {

            // 1) Écrire header CSV
            writeCsvRow(writer, columns);

            XMLStreamReader r = factory.createXMLStreamReader(in);

            boolean inStat = false;

            // stack des éléments courant DANS Stat (path relatif)
            Deque<String> statStack = new ArrayDeque<String>();

            // buffer texte courant
            StringBuilder textBuf = new StringBuilder();

            // record courant: colonne -> valeur
            Map<String, String> currentRow = null;

            while (r.hasNext()) {
                int event = r.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT: {
                        String name = r.getLocalName();

                        if ("Stat".equals(name)) {
                            inStat = true;
                            statStack.clear();
                            currentRow = new HashMap<String, String>();
                            // init à vide (pour garantir colonnes présentes)
                            for (String col : columns) {
                                currentRow.put(col, "");
                            }
                        } else if (inStat) {
                            // on empile le nom de l'élément (relatif à Stat)
                            statStack.addLast(name);
                        }

                        textBuf.setLength(0);
                        break;
                    }

                    case XMLStreamConstants.CHARACTERS: {
                        textBuf.append(r.getText());
                        break;
                    }

                    case XMLStreamConstants.END_ELEMENT: {
                        String name = r.getLocalName();
                        String value = textBuf.toString();
                        if (value != null) value = value.trim();
                        else value = "";

                        if (inStat) {
                            if (!"Stat".equals(name)) {
                                // Chemin courant = statStack + name (élément qui se ferme)
                                List<String> currentPath = new ArrayList<String>(statStack);
                                currentPath.add(name);

                                // On compare avec chaque colonne
                                for (String col : columns) {
                                    // si déjà rempli, on ne réécrit pas (comportement "premier trouvé")
                                    String existing = currentRow.get(col);
                                    if (existing != null && existing.length() > 0) {
                                        continue;
                                    }
                                    List<String> target = colPathSegments.get(col);
                                    if (pathEquals(currentPath, target)) {
                                        currentRow.put(col, value);
                                    }
                                }

                                // on sort d'un élément interne à Stat
                                if (!statStack.isEmpty()) {
                                    statStack.pollLast();
                                }
                            } else {
                                // fin du record Stat => écrire ligne CSV
                                List<String> rowValues = new ArrayList<String>(columns.size());
                                for (String col : columns) {
                                    rowValues.add(nullToEmpty(currentRow.get(col)));
                                }
                                writeCsvRow(writer, rowValues);

                                // reset
                                inStat = false;
                                statStack.clear();
                                currentRow = null;
                            }
                        }

                        textBuf.setLength(0);
                        break;
                    }

                    default:
                        // ignore
                        break;
                }
            }

            r.close();
        }
    }

    // -----------------------
    // Helpers Java 8
    // -----------------------

    private static List<String> splitPath(String path) {
        // Java 8: pas de List.of
        String[] parts = path.split("/");
        List<String> out = new ArrayList<String>(parts.length);
        for (int i = 0; i < parts.length; i++) {
            out.add(parts[i]);
        }
        return out;
    }

    private static boolean pathEquals(List<String> a, List<String> b) {
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!Objects.equals(a.get(i), b.get(i))) return false;
        }
        return true;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static void writeCsvRow(BufferedWriter writer, List<String> values) throws Exception {
        // CSV simple: on escape " et on quote si nécessaire
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(';'); // séparateur ; (souvent mieux pour Excel FR)
            sb.append(escapeCsv(values.get(i)));
        }
        sb.append("\n");
        writer.write(sb.toString());
    }

    private static String escapeCsv(String v) {
        if (v == null) v = "";
        boolean mustQuote = v.indexOf(';') >= 0 || v.indexOf('"') >= 0 || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0;
        String escaped = v.replace("\"", "\"\"");
        if (mustQuote) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    // Exemple d'utilisation
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java StatXmlToCsvExtractor <input.xml> <output.csv>");
            return;
        }
        Path in = new java.io.File(args[0]).toPath();
        Path out = new java.io.File(args[1]).toPath();

        new StatXmlToCsvExtractor().extract(in, out);

        System.out.println("OK -> " + out.toAbsolutePath());
    }
}
