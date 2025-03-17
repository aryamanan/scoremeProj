package com.myproject.pdftableextractor.controller;

import com.myproject.pdftableextractor.model.TableData;
import com.myproject.pdftableextractor.service.PDFTableExtractorService;
import com.myproject.pdftableextractor.service.ExcelExportService;
import com.myproject.pdftableextractor.service.TableValidationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TableExtractorController {

    private static final Logger log = LoggerFactory.getLogger(TableExtractorController.class);

    private final PDFTableExtractorService pdfTableExtractorService;
    private final ExcelExportService excelExportService;
    private final TableValidationService tableValidationService;

    @PostMapping("/extract-table")
    public ResponseEntity<?> extractTable(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Received request to extract table from file: {}", file.getOriginalFilename());
            
            // Extract tables from PDF
            List<TableData> tables = pdfTableExtractorService.extractTablesFromPDF(file);
            
            if (tables.isEmpty()) {
                log.warn("No tables found in the PDF");
                return ResponseEntity.badRequest().body("No tables found in the PDF");
            }

            // Validate extracted tables
            TableValidationService.ValidationResult validationResult = tableValidationService.validateTableData(tables);
            if (!validationResult.isValid()) {
                log.warn("Table validation failed: {}", validationResult.issues());
                return ResponseEntity.badRequest().body(validationResult.issues());
            }

            log.info("Successfully extracted {} tables", tables.size());
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            log.error("Error processing PDF: ", e);
            return ResponseEntity.badRequest().body("Error processing PDF: " + e.getMessage());
        }
    }

    @PostMapping("/extract-and-export")
    public ResponseEntity<?> extractAndExport(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Received request to extract and export table from file: {}", file.getOriginalFilename());
            
            // Extract tables from PDF
            List<TableData> tables = pdfTableExtractorService.extractTablesFromPDF(file);
            
            if (tables.isEmpty()) {
                log.warn("No tables found in the PDF");
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"No tables found in the PDF\"}");
            }

            // Validate extracted tables
            TableValidationService.ValidationResult validationResult = tableValidationService.validateTableData(tables);
            if (!validationResult.isValid()) {
                log.warn("Table validation failed: {}", validationResult.issues());
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"" + String.join(", ", validationResult.issues()) + "\"}");
            }

            // Export to Excel
            byte[] excelFile = excelExportService.exportToExcel(tables);
            
            // Set up response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "bank_statement.xlsx");
            
            log.info("Successfully exported tables to Excel");
            return new ResponseEntity<>(excelFile, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error processing PDF: ", e);
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\": \"Error processing PDF: " + e.getMessage().replace("\"", "'") + "\"}");
        }
    }
} 