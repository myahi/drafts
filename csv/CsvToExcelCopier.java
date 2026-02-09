import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class CsvToExcelCopier {

    /**
     * Copie le contenu d'un CSV dans le dernier onglet d'un fichier Excel (.xlsx).
     *
     * @param csvFilePath         chemin du fichier CSV
     * @param startDataRowNumber  numéro de ligne (1-based) à partir duquel copier les données du CSV
     * @param targetExcelFilePath chemin du fichier Excel cible (.xlsx)
     */
    public static void copyCsvToLastSheet(
            String csvFilePath,
            int startDataRowNumber,
            String targetExcelFilePath
    ) throws IOException {

        if (startDataRowNumber < 1) {
            throw new IllegalArgumentException("startDataRowNumber must be >= 1");
        }

        // Lire toutes les lignes du CSV (UTF-8)
        final List<String> csvLines = Files.readAllLines(
                Paths.get(csvFilePath),
                StandardCharsets.UTF_8
        );

        // Charger le classeur Excel
        try (FileInputStream fis = new FileInputStream(targetExcelFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalStateException("Le fichier Excel ne contient aucun onglet.");
            }

            // Récupérer le dernier onglet
            Sheet sheet = workbook.getSheetAt(workbook.getNumberOfSheets() - 1);

            // Déterminer la première ligne vide dans l'onglet
            int excelRowIndex = sheet.getLastRowNum() + 1;
            // Si la feuille est complètement vide, lastRowNum vaut 0, mais il n'y a pas forcément de ligne 0.
            // On ajuste donc :
            if (sheet.getPhysicalNumberOfRows() == 0) {
                excelRowIndex = 0;
            }

            // Copier les lignes CSV vers Excel
            for (int i = startDataRowNumber - 1; i < csvLines.size(); i++) {
                String line = csvLines.get(i);

                // Séparateur CSV (à adapter si besoin)
                String[] values = line.split(";", -1);

                Row row = sheet.createRow(excelRowIndex++);

                for (int col = 0; col < values.length; col++) {
                    Cell cell = row.createCell(col, CellType.STRING);
                    cell.setCellValue(values[col]);
                }
            }

            // Écrire le fichier Excel (écrase le fichier existant)
            try (FileOutputStream fos = new FileOutputStream(targetExcelFilePath)) {
                workbook.write(fos);
            }
        }
    }
}
