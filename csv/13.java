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
