import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public class CsvToExcelCopier {

    public static void copyCsvToNewSheet(String csvFilePath, int startDataRowNumber, String targetExcelFilePath)
            throws IOException {

        if (startDataRowNumber < 1) throw new IllegalArgumentException("startDataRowNumber must be >= 1");

        List<String> csvLines = Files.readAllLines(Paths.get(csvFilePath), StandardCharsets.UTF_8);

        try (FileInputStream fis = new FileInputStream(targetExcelFilePath);
             Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.createSheet("Import_" + (wb.getNumberOfSheets() + 1));

            int r = 0;
            for (int i = startDataRowNumber - 1; i < csvLines.size(); i++) {
                String[] values = csvLines.get(i).split(";", -1);
                Row row = sheet.createRow(r++);
                for (int c = 0; c < values.length; c++) {
                    row.createCell(c).setCellValue(values[c]);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(targetExcelFilePath)) {
                wb.write(fos);
            }
        }
    }
}
