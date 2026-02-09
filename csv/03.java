public static void copyCsvToNewSheet(
        String csvFilePath,
        int startDataRowNumber,
        String targetExcelFilePath,
        String separator,
        String sheetName
) throws IOException {

    if (startDataRowNumber < 1) {
        throw new IllegalArgumentException("startDataRowNumber must be >= 1");
    }

    List<String> csvLines = Files.readAllLines(
            Paths.get(csvFilePath),
            StandardCharsets.UTF_8
    );

    try (FileInputStream fis = new FileInputStream(targetExcelFilePath);
         Workbook workbook = new XSSFWorkbook(fis)) {

        Sheet sheet = workbook.createSheet(sheetName);

        int rowIndex = 0;
        int maxCols = 0;

        for (int i = startDataRowNumber - 1; i < csvLines.size(); i++) {
            String[] values = csvLines.get(i).split(separator, -1);
            maxCols = Math.max(maxCols, values.length);

            Row row = sheet.createRow(rowIndex++);
            for (int col = 0; col < values.length; col++) {
                row.createCell(col, CellType.STRING).setCellValue(values[col]);
            }
        }

        // Auto-size des colonnes
        for (int c = 0; c < maxCols; c++) {
            sheet.autoSizeColumn(c);
        }

        try (FileOutputStream fos = new FileOutputStream(targetExcelFilePath)) {
            workbook.write(fos);
        }
    }
}
