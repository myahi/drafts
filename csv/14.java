import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class EnrichMemberShipCsvToExcelCopier {

    /****** START SET/GET METHOD, DO NOT MODIFY *****/
    protected String csvFilePath = "";
    protected int startDataRowNumber = 0;
    protected String targetExcelFilePath = "";

    public String getcsvFilePath() {
        return csvFilePath;
    }

    public void setcsvFilePath(String val) {
        csvFilePath = val;
    }

    public int getstartDataRowNumber() {
        return startDataRowNumber;
    }

    public void setstartDataRowNumber(int val) {
        startDataRowNumber = val;
    }

    public String gettargetExcelFilePath() {
        return targetExcelFilePath;
    }

    public void settargetExcelFilePath(String val) {
        targetExcelFilePath = val;
    }
    /****** END SET/GET METHOD, DO NOT MODIFY *****/

    public EnrichMemberShipCsvToExcelCopier() {
    }

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("[-+]?\\d+(?:[\\.,]\\d+)?");

    private static boolean isNumeric(String value) {
        return value != null && NUMERIC_PATTERN.matcher(value.trim()).matches();
    }

    public static void copyCsvToNewSheet(String csvFilePath, int startDataRowNumber, String targetExcelFilePath,
            String separator, String sheetName) throws IOException {

        if (startDataRowNumber < 1) {
            throw new IllegalArgumentException("startDataRowNumber must be >= 1");
        }

        List<String> csvLines = Files.readAllLines(Paths.get(csvFilePath), StandardCharsets.UTF_8);

        try (FileInputStream fis = new FileInputStream(targetExcelFilePath);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.createSheet(sheetName);

            XSSFCellStyle blueCellStyle = workbook.createCellStyle();
            java.awt.Color blue = new java.awt.Color(204, 204, 255);
            blueCellStyle.setFillForegroundColor(new XSSFColor(blue));
            blueCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont boldFont = workbook.createFont();
            boldFont.setBold(true);
            blueCellStyle.setFont(boldFont);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            blueCellStyle.setFont(headerFont);

            DataFormat dataFormat = workbook.createDataFormat();

            XSSFCellStyle integerCellStyle = workbook.createCellStyle();
            integerCellStyle.setDataFormat(dataFormat.getFormat("0"));

            XSSFCellStyle decimalCellStyle = workbook.createCellStyle();
            decimalCellStyle.setDataFormat(dataFormat.getFormat("0.############"));

            int rowIndex = 0;
            int maxCols = 0;

            for (int i = startDataRowNumber - 1; i < csvLines.size(); i++) {
                String[] values = csvLines.get(i).split(separator, -1);
                maxCols = Math.max(maxCols, values.length);
                Row row = sheet.createRow(rowIndex++);

                for (int col = 0; col < values.length; col++) {
                    String value = values[col] == null ? "" : values[col].trim();

                    Cell cell;
                    if (rowIndex == 1) {
                        cell = row.createCell(col, CellType.STRING);
                        cell.setCellValue(value);
                        cell.setCellStyle(blueCellStyle);
                    } else if (isNumeric(value)) {
                        cell = row.createCell(col, CellType.NUMERIC);
                        double numericValue = Double.parseDouble(value.replace(',', '.'));
                        cell.setCellValue(numericValue);

                        if (numericValue == Math.rint(numericValue)) {
                            cell.setCellStyle(integerCellStyle);
                        } else {
                            cell.setCellStyle(decimalCellStyle);
                        }
                    } else {
                        cell = row.createCell(col, CellType.STRING);
                        cell.setCellValue(value);
                    }
                }
            }

            for (int c = 0; c < maxCols; c++) {
                sheet.autoSizeColumn(c);
            }

            try (FileOutputStream fos = new FileOutputStream(targetExcelFilePath)) {
                workbook.write(fos);
            }
        }
    }

    public void invoke() throws Exception {
        if (csvFilePath.contains("_REP00036")) {
            copyCsvToNewSheet(csvFilePath, startDataRowNumber, targetExcelFilePath, "\t", "IM Swapclear Titres LCH LTD");
        } 
        else if (csvFilePath.contains("_REP00031")) {
            copyCsvToNewSheet(csvFilePath, startDataRowNumber, targetExcelFilePath, "\t", "IM VM Swapclear Expo LCH LTD");
        }
        else if (csvFilePath.contains("_REP00032")) {
            copyCsvToNewSheet(csvFilePath, startDataRowNumber, targetExcelFilePath, "\t", "Default Fund SwapClear OTC LCH");
        } else {
            copyCsvToNewSheet(csvFilePath, startDataRowNumber, targetExcelFilePath, "\t", "IM Swapclear Expo LCH LTD");
        }
    }
    
    public static void main(String[] args) throws Exception {
        EnrichMemberShipCsvToExcelCopier tester = new EnrichMemberShipCsvToExcelCopier();
        tester.csvFilePath = "C:/downloads/fred/P-MBNK-POS-20260323-000000_REP00031 - Collateral and Exposure Summary.TXT";
        tester.startDataRowNumber = 1;
        tester.targetExcelFilePath = "C:/downloads/fred/Report_Collateral_Titres_CCP_20260323.xlsx";
        tester.invoke();
        tester.csvFilePath = "C:/downloads/fred/P-MBNK-POS-20260323-000000_REP00022 - Yesterday's Cover Account Postings_ 1.TXT";
        tester.invoke();
        tester.csvFilePath = "C:/downloads/fred/P-MBNK-POS-20250825-000000_REP00036a - SOD Non Cash Collateral Holdings_ 1.TXT";
        tester.invoke();
    }
}
