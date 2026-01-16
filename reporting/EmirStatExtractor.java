import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Java 8 - Extraction streaming de records <Stat> vers Excel (.xlsx)
 *
 * - Parse XML en streaming (StAX)
 * - Chaque <Stat>...</Stat> => 1 ligne Excel
 * - Colonnes configurées via mapping (nomColonne -> chemin XML relatif à Stat)
 */
public class StatXmlToExcelExtractor {

    /**
     * Mapping: Nom de colonne -> chemin XML relatif à <Stat>.
     * Adapte ce mapping à tes champs.
     */
    private static final LinkedHashMap<String, String> COLUMN_NAME_TO_RELATIVE_PATH = new LinkedHashMap<String, String>();
    static {
        COLUMN_NAME_TO_RELATIVE_PATH.put("ID", "CmonTradData/TxData/TxId/Prtry/Id");
        COLUMN_NAME_TO_RELATIVE_PATH.put("LEI_REPORTING_CTP", "CtrPtySpcfcData/CtrPty/RptgCtrPty/Id/Lgl/Id/LEI");
        COLUMN_NAME_TO_RELATIVE_PATH.put("LEI_OTHER_CTP", "CtrPtySpcfcData/CtrPty/OthrCtrPty/IdTp/Lgl/Id/LEI");
    }

    /**
     * Extrait les données depuis xmlInput et écrit un fichier Excel xlsxOutput.
     */
    public void extractToXlsx(Path xmlInput, Path xlsxOutput) throws Exception {

        // Prépare: liste ordonnée des colonnes et paths segmentés
        final List<String> columnNames = new ArrayList<String>(COLUMN_NAME_TO_RELATIVE_PATH.keySet());
        final Map<String, List<String>> columnNameToPathSegments = new HashMap<String, List<String>>();
        for (Map.Entry<String, String> entry : COLUMN_NAME_TO_RELATIVE_PATH.entrySet()) {
            columnNameToPathSegments.put(entry.getKey(), splitPath(entry.getValue()));
        }

        // StAX factory
        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        try {
            xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        } catch (IllegalArgumentException ignore) {
        }

        // Workbook streaming (garde seulement N lignes en mémoire avant flush)
        // 200 est un bon compromis ; tu peux ajuster
        SXSSFWorkbook workbook = new SXSSFWorkbook(200);
        workbook.setCompressTempFiles(true);

        Sheet sheet = workbook.createSheet("Stat");
        int excelRowIndex = 0;

        // Header Excel
        Row headerRow = sheet.createRow(excelRowIndex++);
        for (int i = 0; i < columnNames.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columnNames.get(i));
        }

        try (InputStream xmlStream = Files.newInputStream(xmlInput);
             OutputStream excelStream = Files.newOutputStream(
                     xlsxOutput,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {

            XMLStreamReader xmlReader = xmlInputFactory.createXMLStreamReader(xmlStream);

            boolean isInsideStatBlock = false;

            // pile des éléments ouverts DANS <Stat> (chemin relatif)
            Deque<String> currentRelativePathStack = new ArrayDeque<String>();

            // buffer texte
            StringBuilder currentTextBuffer = new StringBuilder();

            // ligne courante: colonne -> valeur
            Map<String, String> currentRecordValues = null;

            while (xmlReader.hasNext()) {
                int eventType = xmlReader.next();

                switch (eventType) {
                    case XMLStreamConstants.START_ELEMENT: {
                        String elementName = xmlReader.getLocalName();
                        currentTextBuffer.setLength(0);

                        if ("Stat".equals(elementName)) {
                            isInsideStatBlock = true;
                            currentRelativePathStack.clear();

                            currentRecordValues = new HashMap<String, String>();
                            for (String col : columnNames) {
                                currentRecordValues.put(col, "");
                            }
                        } else if (isInsideStatBlock) {
                            // on entre dans un élément du Stat => push dans le chemin
                            currentRelativePathStack.addLast(elementName);
                        }
                        break;
                    }

                    case XMLStreamConstants.CHARACTERS: {
                        currentTextBuffer.append(xmlReader.getText());
                        break;
                    }

                    case XMLStreamConstants.END_ELEMENT: {
                        String elementName = xmlReader.getLocalName();
                        String elementText = safeTrim(currentTextBuffer.toString());

                        if (isInsideStatBlock) {
                            if (!"Stat".equals(elementName)) {
                                // chemin courant = stack (inclut déjà l'élément qu'on ferme)
                                List<String> currentRelativePath = new ArrayList<String>(currentRelativePathStack);

                                // si ce chemin correspond à une colonne ciblée => on stocke
                                for (String columnName : columnNames) {
                                    if (!isEmpty(currentRecordValues.get(columnName))) {
                                        continue; // garde le premier trouvé
                                    }
                                    List<String> targetPath = columnNameToPathSegments.get(columnName);
                                    if (pathEquals(currentRelativePath, targetPath)) {
                                        currentRecordValues.put(columnName, elementText);
                                    }
                                }

                                // fin d'un élément du Stat => pop
                                if (!currentRelativePathStack.isEmpty()) {
                                    currentRelativePathStack.pollLast();
                                }
                            } else {
                                // fin du record Stat => écrire la ligne Excel
                                Row dataRow = sheet.createRow(excelRowIndex++);
                                for (int i = 0; i < columnNames.size(); i++) {
                                    String col = columnNames.get(i);
                                    String val = currentRecordValues.get(col);
                                    dataRow.createCell(i).setCellValue(val == null ? "" : val);
                                }

                                // reset pour le Stat suivant
                                isInsideStatBlock = false;
                                currentRelativePathStack.clear();
                                currentRecordValues = null;
                            }
                        }

                        currentTextBuffer.setLength(0);
                        break;
                    }

                    default:
                        break;
                }
            }

            xmlReader.close();

            // Optionnel: ajuster la largeur (à éviter si très gros, mais 10k ok)
            // Attention: autosize sur SXSSF peut être coûteux si beaucoup de colonnes.
            // for (int i = 0; i < columnNames.size(); i++) sheet.autoSizeColumn(i);

            workbook.write(excelStream);
        } finally {
            // Important: cleanup des fichiers temporaires SXSSF
            workbook.dispose();
            workbook.close();
        }
    }

    // -----------------------
    // Helpers Java 8
    // -----------------------

    private static List<String> splitPath(String path) {
        String[] parts = path.split("/");
        List<String> segments = new ArrayList<String>(parts.length);
        for (int i = 0; i < parts.length; i++) {
            segments.add(parts[i]);
        }
        return segments;
    }

    private static boolean pathEquals(List<String> left, List<String> right) {
        if (left == null || right == null) return false;
        if (left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++) {
            if (!Objects.equals(left.get(i), right.get(i))) return false;
        }
        return true;
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    // Exemple CLI
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java StatXmlToExcelExtractor <input.xml> <output.xlsx>");
            return;
        }
        Path input = new java.io.File(args[0]).toPath();
        Path output = new java.io.File(args[1]).toPath();

        new StatXmlToExcelExtractor().extractToXlsx(input, output);
        System.out.println("OK -> " + output.toAbsolutePath());
    }
}
