package com.myproject.pdftableextractor.service;

import com.myproject.pdftableextractor.model.TableData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TableValidationService {

    public record ValidationResult(boolean isValid, List<String> issues) {}

    public ValidationResult validateTableData(List<TableData> tables) {
        List<String> issues = new ArrayList<>();
        boolean isValid = true;

        for (int i = 0; i < tables.size(); i++) {
            TableData table = tables.get(i);
            
            // Validate table structure
            if (table.getHeaders() == null || table.getHeaders().isEmpty()) {
                issues.add(String.format("Table %d (Page %d): Missing headers", i + 1, table.getPageNumber()));
                isValid = false;
                continue;
            }

            // Validate rows
            if (table.getRows() == null || table.getRows().isEmpty()) {
                issues.add(String.format("Table %d (Page %d): No data rows found", i + 1, table.getPageNumber()));
                isValid = false;
                continue;
            }

            // Validate data consistency
            for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
                Map<String, String> row = table.getRows().get(rowIndex);
                
                // Check if all headers have corresponding values
                for (String header : table.getHeaders()) {
                    if (!row.containsKey(header)) {
                        issues.add(String.format("Table %d (Page %d), Row %d: Missing value for header '%s'", 
                            i + 1, table.getPageNumber(), rowIndex + 1, header));
                        isValid = false;
                    }
                }

                // Check for empty or malformed values
                for (Map.Entry<String, String> entry : row.entrySet()) {
                    String value = entry.getValue();
                    if (value == null || value.trim().isEmpty()) {
                        issues.add(String.format("Table %d (Page %d), Row %d: Empty value for column '%s'", 
                            i + 1, table.getPageNumber(), rowIndex + 1, entry.getKey()));
                    }
                }
            }

            // Validate numerical values in transaction amounts (if present)
            validateTransactionAmounts(table, i, issues);
        }

        if (!isValid) {
            log.warn("Table validation failed with {} issues", issues.size());
            issues.forEach(issue -> log.warn(issue));
        } else {
            log.info("Table validation passed successfully");
        }

        return new ValidationResult(isValid, issues);
    }

    private void validateTransactionAmounts(TableData table, int tableIndex, List<String> issues) {
        // Look for columns that might contain amounts
        List<String> amountColumns = table.getHeaders().stream()
            .filter(header -> header.toLowerCase().contains("amount") || 
                            header.toLowerCase().contains("balance") ||
                            header.toLowerCase().contains("dr") ||
                            header.toLowerCase().contains("cr"))
            .toList();

        for (String amountColumn : amountColumns) {
            for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
                String value = table.getRows().get(rowIndex).get(amountColumn);
                if (value != null && !value.trim().isEmpty()) {
                    try {
                        // Remove common currency symbols and commas
                        String cleanValue = value.replaceAll("[₹,]", "")
                                               .replaceAll("Dr", "")
                                               .replaceAll("Cr", "")
                                               .trim();
                        Double.parseDouble(cleanValue);
                    } catch (NumberFormatException e) {
                        issues.add(String.format("Table %d (Page %d), Row %d: Invalid amount format in column '%s': %s", 
                            tableIndex + 1, table.getPageNumber(), rowIndex + 1, amountColumn, value));
                    }
                }
            }
        }
    }
} 