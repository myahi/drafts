import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelAlertChecker {

    public static void main(String[] args) {
        String filePath = "chemin/vers/ton/fichier.xlsx";

        try {
            boolean alertShouldBeSent = shouldSendAlert(filePath);
            System.out.println("alertShouldBeSent = " + alertShouldBeSent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean shouldSendAlert(String filePath) throws IOException {
        boolean alertShouldBeSent = false;

        File file = new File(filePath);

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);

                if (containsDataBeyondHeader(sheet)) {
                    alertShouldBeSent = true;
                    break;
                }
            }
        }

        return alertShouldBeSent;
    }

    private static boolean containsDataBeyondHeader(Sheet sheet) {
        if (sheet == null) {
            return false;
        }

        int firstRowNum = sheet.getFirstRowNum();
        int lastRowNum = sheet.getLastRowNum();

        // S'il n'y a qu'une seule ligne ou moins, on considère qu'il n'y a que l'en-tête
        if (lastRowNum <= firstRowNum) {
            return false;
        }

        // On vérifie les lignes après l'en-tête
        for (int rowIndex = firstRowNum + 1; rowIndex <= lastRowNum; rowIndex++) {
            Row row = sheet.getRow(rowIndex);

            if (row != null && !isRowEmpty(row)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }

        short firstCellNum = row.getFirstCellNum();
        short lastCellNum = row.getLastCellNum();

        if (firstCellNum == -1) {
            return true;
        }

        for (int cellIndex = firstCellNum; cellIndex < lastCellNum; cellIndex++) {
            if (row.getCell(cellIndex) != null
                    && row.getCell(cellIndex).toString().trim().length() > 0) {
                return false;
            }
        }

        return true;
    }
}
