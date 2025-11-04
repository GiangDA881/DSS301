package g2.g2_gp_project.service;

import g2.g2_gp_project.dto.DataQualityReport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DataQualityService {

    /**
     * Analyze CSV file quality and return detailed report
     */
    public DataQualityReport analyzeFile(MultipartFile file) throws Exception {
        log.info("Starting data quality analysis for file: {}", file.getOriginalFilename());

        DataQualityReport report = DataQualityReport.builder()
                .fileName(file.getOriginalFilename())
                .build();

        Map<String, DataQualityReport.ColumnQuality> columnStats = new HashMap<>();
        List<DataQualityReport.DataIssue> issues = new ArrayList<>();

        long totalRows = 0;
        long validRows = 0;
        long missingCustomerIds = 0;
        long invalidDates = 0;
        long invalidPrices = 0;
        long invalidQuantities = 0;
        long emptyDescriptions = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            // Detect CSV format
            CSVFormat csvFormat = detectCSVFormat(file);

            try (CSVParser csvParser = CSVParser.parse(new BufferedReader(new InputStreamReader(file.getInputStream())), csvFormat)) {

                // Initialize column stats
                for (String header : csvParser.getHeaderNames()) {
                    columnStats.put(header, DataQualityReport.ColumnQuality.builder()
                            .columnName(header)
                            .totalValues(0)
                            .nullValues(0)
                            .emptyValues(0)
                            .invalidValues(0)
                            .build());
                }

                int rowNumber = 1;
                for (CSVRecord record : csvParser) {
                    rowNumber++;
                    totalRows++;
                    boolean rowValid = true;

                    // Check InvoiceNo
                    String invoiceNo = getRecordValue(record, "InvoiceNo");
                    updateColumnStats(columnStats, "InvoiceNo", invoiceNo);
                    if (isNullOrEmpty(invoiceNo)) {
                        addIssue(issues, rowNumber, "InvoiceNo", "MISSING", invoiceNo, "Missing invoice number");
                        rowValid = false;
                    }

                    // Check StockCode
                    String stockCode = getRecordValue(record, "StockCode");
                    updateColumnStats(columnStats, "StockCode", stockCode);
                    if (isNullOrEmpty(stockCode)) {
                        addIssue(issues, rowNumber, "StockCode", "MISSING", stockCode, "Missing stock code");
                        rowValid = false;
                    }

                    // Check Description
                    String description = getRecordValue(record, "Description");
                    updateColumnStats(columnStats, "Description", description);
                    if (isNullOrEmpty(description)) {
                        emptyDescriptions++;
                    }

                    // Check Quantity
                    String quantity = getRecordValue(record, "Quantity");
                    updateColumnStats(columnStats, "Quantity", quantity);
                    if (!isValidInteger(quantity)) {
                        invalidQuantities++;
                        columnStats.get("Quantity").setInvalidValues(columnStats.get("Quantity").getInvalidValues() + 1);
                        if (issues.size() < 100) { // Limit issues to first 100
                            addIssue(issues, rowNumber, "Quantity", "INVALID_NUMBER", quantity, "Invalid quantity format");
                        }
                        rowValid = false;
                    }

                    // Check InvoiceDate
                    String invoiceDate = getRecordValue(record, "InvoiceDate");
                    updateColumnStats(columnStats, "InvoiceDate", invoiceDate);
                    if (!isValidDate(invoiceDate)) {
                        invalidDates++;
                        columnStats.get("InvoiceDate").setInvalidValues(columnStats.get("InvoiceDate").getInvalidValues() + 1);
                        if (issues.size() < 100) {
                            addIssue(issues, rowNumber, "InvoiceDate", "INVALID_DATE", invoiceDate, "Invalid date format");
                        }
                        rowValid = false;
                    }

                    // Check UnitPrice
                    String unitPrice = getRecordValue(record, "UnitPrice");
                    updateColumnStats(columnStats, "UnitPrice", unitPrice);
                    if (!isValidDecimal(unitPrice)) {
                        invalidPrices++;
                        columnStats.get("UnitPrice").setInvalidValues(columnStats.get("UnitPrice").getInvalidValues() + 1);
                        if (issues.size() < 100) {
                            addIssue(issues, rowNumber, "UnitPrice", "INVALID_PRICE", unitPrice, "Invalid price format");
                        }
                        rowValid = false;
                    }

                    // Check CustomerID
                    String customerId = getRecordValue(record, "CustomerID");
                    updateColumnStats(columnStats, "CustomerID", customerId);
                    if (isNullOrEmpty(customerId)) {
                        missingCustomerIds++;
                        if (issues.size() < 100) {
                            addIssue(issues, rowNumber, "CustomerID", "MISSING", customerId, "Missing customer ID - order will be skipped");
                        }
                        rowValid = false;
                    }

                    // Check Country
                    String country = getRecordValue(record, "Country");
                    updateColumnStats(columnStats, "Country", country);

                    if (rowValid) {
                        validRows++;
                    }

                    // Log progress
                    if (rowNumber % 10000 == 0) {
                        log.info("Analyzed {} rows...", rowNumber);
                    }
                }
            }
        }

        // Calculate quality scores
        for (DataQualityReport.ColumnQuality col : columnStats.values()) {
            if (col.getTotalValues() > 0) {
                double validValues = col.getTotalValues() - col.getNullValues() - col.getEmptyValues() - col.getInvalidValues();
                col.setQualityScore((validValues / col.getTotalValues()) * 100);
            }
        }

        // Build report
        report.setTotalRows(totalRows);
        report.setValidRows(validRows);
        report.setInvalidRows(totalRows - validRows);
        report.setColumnQuality(columnStats);
        report.setIssues(issues);
        report.setMissingCustomerIds(missingCustomerIds);
        report.setInvalidDates(invalidDates);
        report.setInvalidPrices(invalidPrices);
        report.setInvalidQuantities(invalidQuantities);
        report.setEmptyDescriptions(emptyDescriptions);

        log.info("Data quality analysis completed. Total: {}, Valid: {}, Invalid: {}",
                totalRows, validRows, totalRows - validRows);

        return report;
    }

    private CSVFormat detectCSVFormat(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String firstLine = reader.readLine();

            if (firstLine == null || firstLine.trim().isEmpty()) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            firstLine = firstLine.replaceAll("[,;\\t]+$", "");

            char delimiter = ',';
            if (firstLine.contains(";")) {
                delimiter = ';';
            } else if (firstLine.contains("\t")) {
                delimiter = '\t';
            }

            String[] headers = firstLine.split(String.valueOf(delimiter));
            List<String> cleanHeaders = new ArrayList<>();
            for (String header : headers) {
                if (header != null && !header.trim().isEmpty()) {
                    cleanHeaders.add(header.trim());
                }
            }

            return CSVFormat.DEFAULT
                    .builder()
                    .setDelimiter(delimiter)
                    .setHeader(cleanHeaders.toArray(new String[0]))
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .setIgnoreSurroundingSpaces(true)
                    .setNullString("")
                    .setAllowMissingColumnNames(true)
                    .build();
        }
    }

    private String getRecordValue(CSVRecord record, String columnName) {
        try {
            if (record.isMapped(columnName)) {
                return record.get(columnName);
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private void updateColumnStats(Map<String, DataQualityReport.ColumnQuality> stats, String columnName, String value) {
        if (!stats.containsKey(columnName)) return;

        DataQualityReport.ColumnQuality col = stats.get(columnName);
        col.setTotalValues(col.getTotalValues() + 1);

        if (value == null) {
            col.setNullValues(col.getNullValues() + 1);
        } else if (value.trim().isEmpty()) {
            col.setEmptyValues(col.getEmptyValues() + 1);
        }
    }

    private void addIssue(List<DataQualityReport.DataIssue> issues, long rowNumber, String columnName,
                          String issueType, String value, String description) {
        issues.add(DataQualityReport.DataIssue.builder()
                .rowNumber(rowNumber)
                .columnName(columnName)
                .issueType(issueType)
                .value(value == null ? "null" : value)
                .description(description)
                .build());
    }

    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null");
    }

    private boolean isValidInteger(String value) {
        if (isNullOrEmpty(value)) return false;
        try {
            Integer.parseInt(value.trim().split("\\.")[0]);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidDecimal(String value) {
        if (isNullOrEmpty(value)) return false;
        try {
            new BigDecimal(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidDate(String value) {
        if (isNullOrEmpty(value)) return false;

        String[] formats = {
            "dd/MM/yyyy H:mm",
            "dd/MM/yyyy HH:mm",
            "M/d/yyyy H:mm",
            "M/d/yyyy HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "MM/dd/yyyy HH:mm"
        };

        for (String format : formats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                LocalDateTime.parse(value.trim(), formatter);
                return true;
            } catch (Exception e) {
                // Try next format
            }
        }

        return false;
    }
}

