import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.xml.stream.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

public class StatExtractor {

    /**
     * Déclare ici les colonnes dans l'ordre souhaité + leur path RELATIF à <Stat>.
     * Le path est une liste de noms d'éléments (localName), séparés par "/".
     *
     * Exemple: "TxData/TxId/Prtry/Id"
     */
    private static final LinkedHashMap<String, String> COLUMN_TO_PATH = new LinkedHashMap<>();
    static {
        COLUMN_TO_PATH.put("ID", "CmonTradData/TxData/TxId/Prtry/Id");
        COLUMN_TO_PATH.put("LEI_REPORTING_CTP", "CtrPtySpcfcData/CtrPty/RptgCtrPty/Id/Lgl/Id/LEI");
        COLUMN_TO_PATH.put("LEI_OTHER_CTP", "CtrPtySpcfcData/CtrPty/OthrCtrPty/IdTp/Lgl/Id/LEI");
        COLUMN_TO_PATH.put("RPTG_TM_STMP", "CtrPtySpcfcData/RptgTmStmp");
        COLUMN_TO_PATH.put("CTRCT_TP", "CmonTradData/CtrctData/CtrctTp");
        COLUMN_TO_PATH.put("ASST_CLSS", "CmonTradData/CtrctData/AsstClss");
        COLUMN_TO_PATH.put("PDCT_CLSSFCTN", "CmonTradData/CtrctData/PdctClssfctn");
        COLUMN_TO_PATH.put("EXCTN_TMSTMP", "CmonTradData/TxData/ExctnTmStmp");
        // Ajoute tes autres champs ici...
    }

    public static void extractToExcel(Path xmlPath, Path xlsxPath) throws Exception {
        // Précompile les paths (String -> List<String>)
        List<String> columns = new ArrayList<>(COLUMN_TO_PATH.keySet());
        Map<String, List<String>> colPathParts = new HashMap<>();
        for (Map.Entry<String, String> e : COLUMN_TO_PATH.entrySet()) {
            colPathParts.put(e.getKey(), List.of(e.getValue().split("/")));
        }

        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);

        try (InputStream in = Files.newInputStream(xmlPath);
             SXSSFWorkbook wb = new SXSSFWorkbook(200);
             OutputStream out = Files.newOutputStream(xlsxPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            Sheet sheet = wb.createSheet("Stat");
            int rowNum = 0;

            // Header
            Row header = sheet.createRow(rowNum++);
            for (int c = 0; c < columns.size(); c++) {
                header.createCell(c).setCellValue(columns.get(c));
            }

            XMLStreamReader r = factory.createXMLStreamReader(in);

            // Stack du chemin courant (localNames)
            Deque<String> stack = new ArrayDeque<>();

            boolean inStat = false;
            // path dans <Stat> uniquement
            Deque<String> statStack = new ArrayDeque<>();

            // Buffer texte courant
            StringBuilder textBuf = new StringBuilder();

            // Row courante (colName -> value)
            Map<String, String> currentRow = null;

            while (r.hasNext()) {
                int event = r.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT -> {
                        String name = r.getLocalName();
                        stack.addLast(name);

                        // Dès qu'on rentre dans Stat, on initialise
                        if ("Stat".equals(name)) {
                            inStat = true;
                            statStack.clear();
                            currentRow = new HashMap<>();
                            for (String col : columns) currentRow.put(col, "");
                        } else if (inStat) {
                            statStack.addLast(name);
                        }

                        textBuf.setLength(0);
                    }

                    case XMLStreamConstants.CHARACTERS -> {
                        textBuf.append(r.getText());
                    }

                    case XMLStreamConstants.END_ELEMENT -> {
                        String name = r.getLocalName();

                        // La valeur du noeud qu'on ferme
                        String value = textBuf.toString().trim();

                        if (inStat) {
                            // On est dans <Stat> : vérifier si le path courant correspond à une colonne
                            // Le path courant à matcher = statStack + name(qui se ferme)
                            List<String> currentPath = new ArrayList<>(statStack);
                            currentPath.add(name);

                            for (String col : columns) {
                                // Si déjà renseigné, on ne réécrit pas (tu peux changer ce comportement si besoin)
                                if (currentRow.get(col) != null && !currentRow.get(col).isEmpty()) continue;

                                List<String> target = colPathParts.get(col);
                                if (pathEquals(currentPath, target)) {
                                    currentRow.put(col, value);
                                }
                            }
                        }

                        // Si on ferme </Stat> : on écrit la ligne
                        if ("Stat".equals(name) && inStat) {
                            Row row = sheet.createRow(rowNum++);
                            for (int c = 0; c < columns.size(); c++) {
                                String col = columns.get(c);
                                row.createCell(c).setCellValue(nullToEmpty(currentRow.get(col)));
                            }
                            inStat = false;
                            statStack.clear();
                            currentRow = null;
                        } else if (inStat) {
                            // on sort d'un élément à l'intérieur de Stat
                            if (!statStack.isEmpty()) {
                                // on enlève le dernier ouvert (celui qu'on ferme) -> en pratique statStack contient le START_ELEMENT,
                                // et on enlève maintenant qu'on termine son END_ELEMENT.
                                statStack.pollLast();
                            }
                        }

                        // pop stack global
                        if (!stack.isEmpty()) stack.pollLast();

                        textBuf.setLength(0);
                    }

                    default -> { /* ignore */ }
                }
            }

            wb.write(out);
            wb.dispose();
        }
    }

    private static boolean pathEquals(List<String> current, List<String> target) {
        if (current.size() != target.size()) return false;
        for (int i = 0; i < current.size(); i++) {
            if (!Objects.equals(current.get(i), target.get(i))) return false;
        }
        return true;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    // Exemple d'utilisation
    public static void main(String[] args) throws Exception {
        Path xml = Paths.get("input.xml");
        Path xlsx = Paths.get("output.xlsx");
        extractToExcel(xml, xlsx);
        System.out.println("OK: " + xlsx);
    }
}
