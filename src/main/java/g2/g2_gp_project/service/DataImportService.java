package g2.g2_gp_project.service;

import g2.g2_gp_project.dto.DataImportResult;
import g2.g2_gp_project.entity.*;
import g2.g2_gp_project.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataImportService {

    private final RawTransactionRepository rawTransactionRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * Main method to process and load file data through ETL pipeline
     */
    public DataImportResult processAndLoadFile(MultipartFile file) {
        log.info("Starting ETL process for file: {}", file.getOriginalFilename());

        DataImportResult result = DataImportResult.builder()
                .fileName(file.getOriginalFilename())
                .success(false)
                .build();

        try {
            // Validate file first
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }

            // Check MongoDB connection
            try {
                rawTransactionRepository.count();
            } catch (Exception e) {
                throw new IllegalStateException("Cannot connect to MongoDB. Please ensure MongoDB is running on localhost:27017");
            }

            // Check PostgreSQL connection
            try {
                customerRepository.count();
            } catch (Exception e) {
                throw new IllegalStateException("Cannot connect to PostgreSQL. Please ensure PostgreSQL is running on localhost:5432");
            }

            // Step 1: Extract and Load to MongoDB (Raw Data)
            List<RawTransaction> rawTransactions = extractAndLoadToMongo(file);
            result.setRawRecordsLoaded(rawTransactions.size());
            log.info("Step 1 (E,L): Loaded {} raw records to MongoDB", rawTransactions.size());

            // Step 2: Transform and Load to PostgreSQL (Clean Data)
            transformAndLoadToPostgres(result);
            log.info("Step 2 (T,L): Processed data to PostgreSQL");

            result.setSuccess(true);
            result.setMessage(String.format(
                "Successfully loaded %d raw records to MongoDB. " +
                "Processed %d customers, %d products, %d orders, %d order items to PostgreSQL. " +
                "Errors: %d",
                result.getRawRecordsLoaded(),
                result.getCustomersProcessed(),
                result.getProductsProcessed(),
                result.getOrdersProcessed(),
                result.getOrderItemsProcessed(),
                result.getErrorCount()
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Validation error: {}", e.getMessage());
            result.setSuccess(false);
            result.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Error during ETL process", e);
            result.setSuccess(false);
            result.setMessage("Error: " + e.getMessage() + ". Please check server logs for details.");
        }

        return result;
    }

    /**
     * Step 1: Extract data from file and Load to MongoDB (Raw staging)
     */
    private List<RawTransaction> extractAndLoadToMongo(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        List<RawTransaction> rawTransactions = new ArrayList<>();
        LocalDateTime importTime = LocalDateTime.now();

        if (fileName == null) {
            throw new IllegalArgumentException("File name is null");
        }

        if (fileName.toLowerCase().endsWith(".csv")) {
            rawTransactions = parseCSV(file, fileName, importTime);
        } else if (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".xls")) {
            rawTransactions = parseExcel(file, fileName, importTime);
        } else {
            throw new IllegalArgumentException("Unsupported file format. Only CSV and Excel files are supported.");
        }

        // Save all raw transactions to MongoDB
        return rawTransactionRepository.saveAll(rawTransactions);
    }

    /**
     * Parse CSV file and extract raw data
     */
    private List<RawTransaction> parseCSV(MultipartFile file, String fileName, LocalDateTime importTime) throws Exception {
        List<RawTransaction> rawTransactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            // Read first line to detect delimiter
            String firstLine = reader.readLine();
            if (firstLine == null || firstLine.trim().isEmpty()) {
                throw new IllegalArgumentException("CSV file is empty or has no header");
            }

            // Detect delimiter (comma or semicolon)
            char delimiter = ',';
            if (firstLine.contains(";")) {
                delimiter = ';';
                log.info("Detected semicolon (;) as CSV delimiter");
            } else {
                log.info("Using comma (,) as CSV delimiter");
            }

            // Create CSVFormat with detected delimiter
            CSVFormat csvFormat = CSVFormat.DEFAULT
                    .builder()
                    .setDelimiter(delimiter)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .build();

            // Reset reader to beginning
            reader.close();
        }

        // Re-open file and parse with correct delimiter
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser csvParser = CSVParser.parse(reader, detectCSVFormat(file))) {

            int rowNumber = 1; // Start after header
            for (CSVRecord record : csvParser) {
                rowNumber++;
                try {
                    RawTransaction raw = new RawTransaction();
                    raw.setInvoiceNo(getRecordValue(record, "InvoiceNo"));
                    raw.setStockCode(getRecordValue(record, "StockCode"));
                    raw.setDescription(getRecordValue(record, "Description"));
                    raw.setQuantity(getRecordValue(record, "Quantity"));
                    raw.setInvoiceDate(getRecordValue(record, "InvoiceDate"));
                    raw.setUnitPrice(getRecordValue(record, "UnitPrice"));
                    raw.setCustomerId(getRecordValue(record, "CustomerID"));
                    raw.setCountry(getRecordValue(record, "Country"));
                    raw.setImportedAt(importTime);
                    raw.setFileName(fileName);
                    raw.setRowNumber(rowNumber);

                    rawTransactions.add(raw);

                    // Log progress for large files
                    if (rowNumber % 10000 == 0) {
                        log.info("Parsed {} rows from CSV", rowNumber);
                    }
                } catch (Exception e) {
                    log.warn("Error parsing CSV row {}: {}", rowNumber, e.getMessage());
                }
            }
        }

        log.info("Successfully parsed {} transactions from CSV file", rawTransactions.size());
        return rawTransactions;
    }

    /**
     * Detect CSV format by reading first line
     */
    private CSVFormat detectCSVFormat(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String firstLine = reader.readLine();

            if (firstLine == null || firstLine.trim().isEmpty()) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            // Clean up header line - remove trailing delimiters
            firstLine = firstLine.replaceAll("[,;\\t]+$", "");

            // Detect delimiter
            char delimiter = ',';
            if (firstLine.contains(";")) {
                delimiter = ';';
                log.info("Detected semicolon (;) as CSV delimiter");
            } else if (firstLine.contains(",")) {
                delimiter = ',';
                log.info("Detected comma (,) as CSV delimiter");
            } else if (firstLine.contains("\t")) {
                delimiter = '\t';
                log.info("Detected tab as CSV delimiter");
            }

            // Count non-empty headers
            String[] headers = firstLine.split(String.valueOf(delimiter));
            List<String> cleanHeaders = new ArrayList<>();
            for (String header : headers) {
                if (header != null && !header.trim().isEmpty()) {
                    cleanHeaders.add(header.trim());
                }
            }

            log.info("Detected {} columns in CSV: {}", cleanHeaders.size(), String.join(", ", cleanHeaders));

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

    /**
     * Safely get record value by column name
     */
    private String getRecordValue(CSVRecord record, String columnName) {
        try {
            if (record.isMapped(columnName)) {
                return record.get(columnName);
            } else {
                log.warn("Column '{}' not found in CSV, trying case-insensitive match", columnName);
                // Try to find column with case-insensitive match
                for (String header : record.getParser().getHeaderNames()) {
                    if (header.equalsIgnoreCase(columnName)) {
                        return record.get(header);
                    }
                }
                return "";
            }
        } catch (Exception e) {
            log.warn("Error getting value for column '{}': {}", columnName, e.getMessage());
            return "";
        }
    }

    /**
     * Parse Excel file and extract raw data
     */
    private List<RawTransaction> parseExcel(MultipartFile file, String fileName, LocalDateTime importTime) throws Exception {
        List<RawTransaction> rawTransactions = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Get header row to find column indices
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> columnMap = new HashMap<>();
            for (Cell cell : headerRow) {
                columnMap.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
            }

            // Process data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    RawTransaction raw = new RawTransaction();
                    raw.setInvoiceNo(getCellValueAsString(row, columnMap.get("InvoiceNo")));
                    raw.setStockCode(getCellValueAsString(row, columnMap.get("StockCode")));
                    raw.setDescription(getCellValueAsString(row, columnMap.get("Description")));
                    raw.setQuantity(getCellValueAsString(row, columnMap.get("Quantity")));
                    raw.setInvoiceDate(getCellValueAsString(row, columnMap.get("InvoiceDate")));
                    raw.setUnitPrice(getCellValueAsString(row, columnMap.get("UnitPrice")));
                    raw.setCustomerId(getCellValueAsString(row, columnMap.get("CustomerID")));
                    raw.setCountry(getCellValueAsString(row, columnMap.get("Country")));
                    raw.setImportedAt(importTime);
                    raw.setFileName(fileName);
                    raw.setRowNumber(i + 1);

                    rawTransactions.add(raw);
                } catch (Exception e) {
                    log.warn("Error parsing Excel row {}: {}", i + 1, e.getMessage());
                }
            }
        }

        return rawTransactions;
    }

    /**
     * Helper method to get cell value as string
     */
    private String getCellValueAsString(Row row, Integer columnIndex) {
        if (columnIndex == null) return "";
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    /**
     * Step 2: Transform raw data and Load to PostgreSQL (Clean data)
     */
    @Transactional
    public void transformAndLoadToPostgres(DataImportResult result) {
        log.info("Starting transformation and loading to PostgreSQL...");
        List<RawTransaction> rawTransactions = rawTransactionRepository.findAll();
        log.info("Retrieved {} raw transactions from MongoDB", rawTransactions.size());

        Map<String, Customer> customerMap = new HashMap<>();
        Map<String, Product> productMap = new HashMap<>();
        Map<String, Order> orderMap = new HashMap<>();
        List<OrderItem> orderItems = new ArrayList<>();

        int errorCount = 0;
        int processedCount = 0;

        for (RawTransaction raw : rawTransactions) {
            try {
                processedCount++;
                if (processedCount % 1000 == 0) {
                    log.info("Processed {} / {} records", processedCount, rawTransactions.size());
                }

                // Transform and create/update Customer
                String customerId = raw.getCustomerId();
                if (customerId != null && !customerId.trim().isEmpty() && !customerId.equalsIgnoreCase("null")) {
                    Customer customer = customerMap.computeIfAbsent(customerId, id -> {
                        return customerRepository.findByCustomerId(id)
                                .orElseGet(() -> {
                                    Customer newCustomer = new Customer();
                                    newCustomer.setCustomerId(id);
                                    newCustomer.setCountry(raw.getCountry());
                                    return newCustomer;
                                });
                    });

                    // Update country if needed
                    if (raw.getCountry() != null && !raw.getCountry().isEmpty()) {
                        customer.setCountry(raw.getCountry());
                    }
                    customerMap.put(customerId, customer);
                }

                // Transform and create/update Product
                String stockCode = raw.getStockCode();
                if (stockCode != null && !stockCode.trim().isEmpty()) {
                    Product product = productMap.computeIfAbsent(stockCode, code -> {
                        return productRepository.findByStockCode(code)
                                .orElseGet(() -> {
                                    Product newProduct = new Product();
                                    newProduct.setStockCode(code);
                                    newProduct.setDescription(raw.getDescription());
                                    newProduct.setUnitPrice(parseBigDecimal(raw.getUnitPrice()));
                                    return newProduct;
                                });
                    });

                    // Update product info if needed
                    if (raw.getDescription() != null && !raw.getDescription().isEmpty()) {
                        product.setDescription(raw.getDescription());
                    }
                    if (raw.getUnitPrice() != null) {
                        product.setUnitPrice(parseBigDecimal(raw.getUnitPrice()));
                    }
                    productMap.put(stockCode, product);
                }

                // Transform and create Order
                String invoiceNo = raw.getInvoiceNo();
                if (invoiceNo != null && !invoiceNo.trim().isEmpty()) {
                    // Only create order if customer exists
                    String custId = raw.getCustomerId();
                    if (custId == null || custId.trim().isEmpty() || custId.equalsIgnoreCase("null") || !customerMap.containsKey(custId)) {
                        log.warn("Skipping order {} - no valid customer (CustomerID: {})", invoiceNo, custId);
                        continue; // Skip this transaction if no valid customer
                    }

                    Order order = orderMap.computeIfAbsent(invoiceNo, invoice -> {
                        return orderRepository.findByOrderId(invoice)
                                .orElseGet(() -> {
                                    Order newOrder = new Order();
                                    newOrder.setOrderId(invoice);
                                    newOrder.setOrderDate(parseDateTime(raw.getInvoiceDate()));
                                    newOrder.setStatus("Completed");

                                    // Set customer for order (guaranteed to exist now)
                                    Customer customer = customerMap.get(custId);
                                    newOrder.setCustomer(customer);

                                    return newOrder;
                                });
                    });
                    orderMap.put(invoiceNo, order);

                    // Create OrderItem
                    if (stockCode != null && !stockCode.trim().isEmpty()) {
                        OrderItem orderItem = new OrderItem();
                        orderItem.setOrder(order);
                        orderItem.setProduct(productMap.get(stockCode));
                        orderItem.setQuantity(parseInteger(raw.getQuantity()));
                        orderItem.setUnitPrice(parseBigDecimal(raw.getUnitPrice()));

                        orderItems.add(orderItem);
                    }
                }

            } catch (Exception e) {
                log.error("Error transforming raw transaction at row {}: {}", raw.getRowNumber(), e.getMessage());
                errorCount++;
            }
        }

        // Save all entities to PostgreSQL in batches
        log.info("Saving {} customers to PostgreSQL...", customerMap.size());
        List<Customer> customers = new ArrayList<>(customerMap.values());
        customers = customerRepository.saveAll(customers);
        result.setCustomersProcessed(customers.size());
        log.info("Saved {} customers", customers.size());

        log.info("Saving {} products to PostgreSQL...", productMap.size());
        List<Product> products = new ArrayList<>(productMap.values());
        products = productRepository.saveAll(products);
        result.setProductsProcessed(products.size());
        log.info("Saved {} products", products.size());

        log.info("Saving {} orders to PostgreSQL...", orderMap.size());
        List<Order> orders = new ArrayList<>(orderMap.values());

        // Calculate totals for each order before saving
        for (Order order : orders) {
            BigDecimal subtotal = BigDecimal.ZERO;
            for (OrderItem item : orderItems) {
                if (item.getOrder().getOrderId().equals(order.getOrderId())) {
                    BigDecimal itemTotal = item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
                    subtotal = subtotal.add(itemTotal);
                }
            }
            order.setSubtotal(subtotal);
            order.setTotalAmount(subtotal); // Can add tax/shipping later
        }

        orders = orderRepository.saveAll(orders);
        result.setOrdersProcessed(orders.size());
        log.info("Saved {} orders", orders.size());

        log.info("Saving {} order items to PostgreSQL...", orderItems.size());
        orderItems = orderItemRepository.saveAll(orderItems);
        result.setOrderItemsProcessed(orderItems.size());
        log.info("Saved {} order items", orderItems.size());

        result.setErrorCount(errorCount);
        log.info("Transformation completed. Errors: {}", errorCount);
    }

    /**
     * Helper method to parse BigDecimal
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Error parsing BigDecimal: {}", value);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Helper method to parse Integer
     */
    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim().split("\\.")[0]); // Handle decimal values
        } catch (NumberFormatException e) {
            log.warn("Error parsing Integer: {}", value);
            return 0;
        }
    }

    /**
     * Helper method to parse DateTime
     */
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return LocalDateTime.now();
        }

        // Try common date formats
        String[] formats = {
            "dd/MM/yyyy H:mm",        // 18/08/2011 6:30 - European format (most common in your data)
            "dd/MM/yyyy HH:mm",       // 18/08/2011 08:30
            "M/d/yyyy H:mm",          // 8/18/2011 6:30 - US format
            "M/d/yyyy HH:mm",         // 8/18/2011 08:30
            "yyyy-MM-dd HH:mm:ss",    // 2011-08-18 08:30:00
            "yyyy-MM-dd'T'HH:mm:ss",  // 2011-08-18T08:30:00
            "MM/dd/yyyy HH:mm"        // 08/18/2011 08:30
        };

        for (String format : formats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return LocalDateTime.parse(value.trim(), formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }

        log.warn("Error parsing DateTime: {}, using current time", value);
        return LocalDateTime.now();
    }
}

