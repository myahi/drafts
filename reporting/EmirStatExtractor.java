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

public class StatXmlToCsvExtractor {

    // 3 champs de ton exemple
    private static final LinkedHashMap<String, String> COLUMN_TO_PATH = new LinkedHashMap<String, String>();
    static {
        COLUMN_TO_PATH.put("ID", "CmonTradData/TxData/TxId/Prtry/Id");
        COLUMN_TO_PATH.put("LEI_REPORTING_CTP", "CtrPtySpcfcData/CtrPty/RptgCtrPty/Id/Lgl/Id/LEI");
        COLUMN_TO_PATH.put("LEI_OTHER_CTP", "CtrPtySpcfcData/CtrPty/OthrCtrPty/IdTp/Lgl/Id/LEI");
    }

    public void extract(Path xmlInput, Path csvOutput) throws Exception {

        final List<String> columns = new ArrayList<String>(COLUMN_TO_PATH.keySet());
        final Map<String, List<String>> colPathSegments = new HashMap<String, List<String>>();
        for (Map.Entry<String, String> e : COLUMN_TO_PATH.entrySet()) {
            colPathSegments.put(e.getKey(), splitPath(e.getValue()));
        }

        XMLInputFactory factory = XMLInputFactory.newFactory();
        try {
            factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        } catch (IllegalArgumentException ignore) {
        }

        try (InputStream in = Files.newInputStream(xmlInput);
             BufferedWriter writer = Files.newBufferedWriter(
                     csvOutput,
                     StandardCharsets.UTF_8,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {

            // Header
            writeCsvRow(writer, columns);

            XMLStreamReader r = factory.createXMLStreamReader(in);

            boolean inStat = false;

            // Chemin courant à l'intérieur de <Stat> (inclut l'élément courant)
            Deque<String> statStack = new ArrayDeque<String>();

            // Buffer texte
            StringBuilder textBuf = new StringBuilder();

            Map<String, String> currentRow = null;

            while (r.hasNext()) {
                int event = r.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT: {
                        String name = r.getLocalName();
                        textBuf.setLength(0);

                        if ("Stat".equals(name)) {
                            inStat = true;
                            statStack.clear();

                            currentRow = new HashMap<String, String>();
                            for (String col : columns) {
                                currentRow.put(col, "");
                            }
                        } else if (inStat) {
                            // push l'élément courant dans le chemin
                            statStack.addLast(name);
                        }
                        break;
                    }

                    case XMLStreamConstants.CHARACTERS: {
                        // Accumule le texte (coalescing ou pas)
                        textBuf.append(r.getText());
                        break;
                    }

                    case XMLStreamConstants.END_ELEMENT: {
                        String name = r.getLocalName();
                        String value = textBuf.toString();
                        value = value == null ? "" : value.trim();

                        if (inStat) {
                            if (!"Stat".equals(name)) {
                                // On est en fin d'un élément interne à Stat.
                                // Le chemin courant = statStack (il inclut déjà l'élément qui se ferme)
                                List<String> currentPath = new ArrayList<String>(statStack);

                                for (String col : columns) {
                                    String existing = currentRow.get(col);
                                    if (existing != null && existing.length() > 0) {
                                        continue; // garde le premier match
                                    }
                                    List<String> target = colPathSegments.get(col);
                                    if (pathEquals(currentPath, target)) {
                                        currentRow.put(col, value);
                                    }
                                }

                                // pop l'élément qui se ferme
                                if (!statStack.isEmpty()) {
                                    statStack.pollLast();
                                }
                            } else {
                                // fin de Stat => écrire la ligne
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
                        break;
                }
            }

            r.close();
        }
    }

    // ---------------- Helpers Java 8 ----------------

    private static List<String> splitPath(String path) {
        String[] parts = path.split("/");
        List<String> out = new ArrayList<String>(parts.length);
        for (int i = 0; i < parts.length; i++) out.add(parts[i]);
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
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(';'); // ; pour Excel FR
            sb.append(escapeCsv(values.get(i)));
        }
        sb.append("\n");
        writer.write(sb.toString());
    }

    private static String escapeCsv(String v) {
        if (v == null) v = "";
        boolean mustQuote = v.indexOf(';') >= 0 || v.indexOf('"') >= 0 || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0;
        String escaped = v.replace("\"", "\"\"");
        return mustQuote ? ("\"" + escaped + "\"") : escaped;
    }

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
