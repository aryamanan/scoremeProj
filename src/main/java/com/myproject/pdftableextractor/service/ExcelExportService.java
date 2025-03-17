package com.myproject.pdftableextractor.service;

import com.myproject.pdftableextractor.model.TableData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ExcelExportService {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^[₹]?\\s*[\\d,]+\\.?\\d*\\s*(Dr|Cr)?$");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{2}[-/]\\w{3}[-/]\\d{4}|\\d{2}/\\d{2}/\\d{4}");

    public byte[] exportToExcel(List<TableData> tables) throws IOException {
        log.info("Starting Excel export for {} tables", tables.size());
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle amountStyle = createAmountStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle wrapStyle = createWrapStyle(workbook);
            
            for (int i = 0; i < tables.size(); i++) {
                TableData table = tables.get(i);
                String sheetName = String.format("Page %d Table %d", table.getPageNumber(), i + 1);
                Sheet sheet = workbook.createSheet(sheetName);
                log.debug("Creating sheet: {}", sheetName);
                
                // Create header row
                Row headerRow = sheet.createRow(0);
                List<String> headers = table.getHeaders();
                for (int col = 0; col < headers.size(); col++) {
                    Cell cell = headerRow.createCell(col);
                    cell.setCellValue(headers.get(col));
                    cell.setCellStyle(headerStyle);
                }
                
                // Create data rows
                List<Map<String, String>> rows = table.getRows();
                for (int rowNum = 0; rowNum < rows.size(); rowNum++) {
                    Row row = sheet.createRow(rowNum + 1);
                    Map<String, String> rowData = rows.get(rowNum);
                    
                    for (int col = 0; col < headers.size(); col++) {
                        String header = headers.get(col);
                        String value = rowData.getOrDefault(header, "").trim();
                        
                        if (value.isEmpty()) {
                            continue;
                        }

                        Cell cell = row.createCell(col);
                        
                        // Apply appropriate formatting based on content
                        if (isAmount(value)) {
                            cell.setCellStyle(amountStyle);
                            String cleanValue = value.replaceAll("[₹,]", "")
                                                   .replaceAll("Dr|Cr", "")
                                                   .trim();
                            try {
                                cell.setCellValue(Double.parseDouble(cleanValue));
                            } catch (NumberFormatException e) {
                                cell.setCellValue(value);
                            }
                        } else if (isDate(value)) {
                            cell.setCellStyle(dateStyle);
                            cell.setCellValue(value);
                        } else {
                            cell.setCellStyle(wrapStyle);
                            cell.setCellValue(value);
                        }
                    }
                }
                
                // Autosize columns
                for (int col = 0; col < headers.size(); col++) {
                    sheet.autoSizeColumn(col);
                    // Add a little extra width for better readability
                    int currentWidth = sheet.getColumnWidth(col);
                    sheet.setColumnWidth(col, (int)(currentWidth * 1.2));
                }
                
                // Freeze header row
                sheet.createFreezePane(0, 1);
            }
            
            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            log.info("Excel export completed successfully");
            return outputStream.toByteArray();
        }
    }
    
    private boolean isAmount(String value) {
        return AMOUNT_PATTERN.matcher(value).matches();
    }
    
    private boolean isDate(String value) {
        return DATE_PATTERN.matcher(value).matches();
    }
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        style.setFont(headerFont);
        
        return style;
    }
    
    private CellStyle createAmountStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("dd-MMM-yyyy"));
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createWrapStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
} 