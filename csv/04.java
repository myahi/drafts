// Style en-tête
CellStyle headerStyle = workbook.createCellStyle();
headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

Font headerFont = workbook.createFont();
headerFont.setBold(true);
headerStyle.setFont(headerFont);


Cell cell = row.createCell(col, CellType.STRING);
cell.setCellValue(values[col]);

if (rowIndex == 1) { // première ligne créée = en-tête
    cell.setCellStyle(headerStyle);
}
