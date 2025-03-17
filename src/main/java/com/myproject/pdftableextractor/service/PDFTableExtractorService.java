package com.myproject.pdftableextractor.service;

import com.myproject.pdftableextractor.model.TableData;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PDFTableExtractorService {

    private static final float COLUMN_DETECTION_THRESHOLD = 5.0f;
    private static final float ROW_DETECTION_THRESHOLD = 5.0f;

    public List<TableData> extractTablesFromPDF(MultipartFile pdfFile) throws IOException {
        log.info("Starting PDF extraction for file: {}", pdfFile.getOriginalFilename());
        List<TableData> tables = new ArrayList<>();
        
        try (PDDocument document = Loader.loadPDF(pdfFile.getBytes())) {
            log.info("PDF loaded successfully. Number of pages: {}", document.getNumberOfPages());
            
            for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
                log.debug("Processing page {}", pageNum + 1);
                CustomPDFTextStripper stripper = new CustomPDFTextStripper();
                stripper.setStartPage(pageNum + 1);
                stripper.setEndPage(pageNum + 1);
                
                // Get text with positions
                List<TextElement> textElements = stripper.getTextElements(document);
                log.debug("Found {} text elements on page {}", textElements.size(), pageNum + 1);
                
                // Filter out separator lines
                textElements = textElements.stream()
                    .filter(element -> !element.getText().trim().matches("^[-]+$"))
                    .collect(Collectors.toList());
                
                // Detect and extract tables
                List<TableData> pageTables = detectTables(textElements, pageNum + 1);
                log.info("Found {} tables on page {}", pageTables.size(), pageNum + 1);
                tables.addAll(pageTables);
            }
        } catch (Exception e) {
            log.error("Error processing PDF: ", e);
            throw e;
        }
        
        return tables;
    }

    private List<TableData> detectTables(List<TextElement> textElements, int pageNumber) {
        // Sort elements by Y position to group into rows
        Map<Float, List<TextElement>> rowGroups = textElements.stream()
            .collect(Collectors.groupingBy(
                element -> roundToNearest(element.getY(), ROW_DETECTION_THRESHOLD),
                TreeMap::new,
                Collectors.toList()
            ));

        // Detect column positions
        List<Float> columnPositions = detectColumnPositions(textElements);
        
        // Group elements into tables based on spacing
        List<List<Float>> tableRegions = detectTableRegions(rowGroups);
        
        return tableRegions.stream()
            .map(region -> extractTableFromRegion(region, rowGroups, columnPositions, pageNumber))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private List<Float> detectColumnPositions(List<TextElement> textElements) {
        // Create histogram of X positions
        Map<Float, Long> xPositionCounts = textElements.stream()
            .map(element -> roundToNearest(element.getX(), COLUMN_DETECTION_THRESHOLD))
            .collect(Collectors.groupingBy(x -> x, Collectors.counting()));
        
        // Find X positions that appear frequently (potential column starts)
        return xPositionCounts.entrySet().stream()
            .filter(entry -> entry.getValue() > 2) // Minimum occurrences to be considered a column
            .map(Map.Entry::getKey)
            .sorted()
            .collect(Collectors.toList());
    }

    private TableData extractTableFromRegion(List<Float> region, Map<Float, List<TextElement>> rowGroups, 
                                          List<Float> columnPositions, int pageNumber) {
        if (region.isEmpty() || columnPositions.isEmpty()) {
            return null;
        }

        // Get the first row as headers
        List<TextElement> headerElements = rowGroups.get(region.get(0));
        
        // Create headers based on column positions
        List<String> headers = new ArrayList<>();
        List<StringBuilder> headerBuilders = new ArrayList<>();
        for (int i = 0; i < columnPositions.size(); i++) {
            headerBuilders.add(new StringBuilder());
        }
        
        for (TextElement element : headerElements) {
            int columnIndex = findNearestColumn(element.getX(), columnPositions);
            if (columnIndex >= 0 && columnIndex < headerBuilders.size()) {
                if (headerBuilders.get(columnIndex).length() > 0) {
                    headerBuilders.get(columnIndex).append(" ");
                }
                headerBuilders.get(columnIndex).append(element.getText().trim());
            }
        }
        
        headers = headerBuilders.stream()
            .map(StringBuilder::toString)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());

        // Process each row
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < region.size(); i++) {
            List<TextElement> rowElements = rowGroups.get(region.get(i));
            Map<String, String> row = new HashMap<>();
            List<StringBuilder> columnBuilders = new ArrayList<>();
            
            for (int j = 0; j < columnPositions.size(); j++) {
                columnBuilders.add(new StringBuilder());
            }
            
            // Group text elements by column
            for (TextElement element : rowElements) {
                int columnIndex = findNearestColumn(element.getX(), columnPositions);
                if (columnIndex >= 0 && columnIndex < columnBuilders.size()) {
                    if (columnBuilders.get(columnIndex).length() > 0) {
                        columnBuilders.get(columnIndex).append(" ");
                    }
                    columnBuilders.get(columnIndex).append(element.getText().trim());
                }
            }
            
            // Create row data
            for (int j = 0; j < Math.min(headers.size(), columnBuilders.size()); j++) {
                String value = columnBuilders.get(j).toString().trim();
                if (!value.isEmpty()) {
                    row.put(headers.get(j), value);
                }
            }
            
            if (!row.isEmpty()) {
                rows.add(row);
            }
        }

        return rows.isEmpty() ? null : TableData.builder()
            .headers(headers)
            .rows(rows)
            .pageNumber(pageNumber)
            .build();
    }

    private int findNearestColumn(float x, List<Float> columnPositions) {
        for (int i = 0; i < columnPositions.size(); i++) {
            float columnX = columnPositions.get(i);
            float nextColumnX = i < columnPositions.size() - 1 ? 
                columnPositions.get(i + 1) : Float.MAX_VALUE;
            
            if (Math.abs(x - columnX) <= COLUMN_DETECTION_THRESHOLD ||
                (x > columnX && x < nextColumnX)) {
                return i;
            }
        }
        return -1;
    }

    private List<List<Float>> detectTableRegions(Map<Float, List<TextElement>> rowGroups) {
        List<List<Float>> tableRegions = new ArrayList<>();
        List<Float> currentRegion = null;
        Float previousY = null;

        for (Float y : rowGroups.keySet()) {
            if (previousY == null) {
                currentRegion = new ArrayList<>();
                currentRegion.add(y);
            } else {
                float gap = y - previousY;
                if (gap > ROW_DETECTION_THRESHOLD * 3) { // Large gap indicates table boundary
                    if (currentRegion != null && currentRegion.size() >= 2) {
                        tableRegions.add(currentRegion);
                    }
                    currentRegion = new ArrayList<>();
                }
                currentRegion.add(y);
            }
            previousY = y;
        }

        if (currentRegion != null && currentRegion.size() >= 2) {
            tableRegions.add(currentRegion);
        }

        return tableRegions;
    }

    private float roundToNearest(float value, float threshold) {
        return Math.round(value / threshold) * threshold;
    }

    private static class TextElement {
        private final String text;
        private final float x;
        private final float y;

        public TextElement(String text, float x, float y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }

        public String getText() { return text; }
        public float getX() { return x; }
        public float getY() { return y; }
    }

    private static class CustomPDFTextStripper extends PDFTextStripper {
        private List<TextElement> textElements;

        public CustomPDFTextStripper() throws IOException {
            super();
            textElements = new ArrayList<>();
        }

        public List<TextElement> getTextElements(PDDocument document) throws IOException {
            textElements.clear();
            getText(document);
            return new ArrayList<>(textElements);
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            if (textPositions == null || textPositions.isEmpty()) return;

            TextPosition firstPosition = textPositions.get(0);
            textElements.add(new TextElement(
                text,
                firstPosition.getXDirAdj(),
                firstPosition.getYDirAdj()
            ));
        }
    }
} 