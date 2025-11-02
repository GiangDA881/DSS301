package g2.g2_gp_project.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import g2.g2_gp_project.entity.Customer;
import g2.g2_gp_project.entity.Order;
import g2.g2_gp_project.entity.OrderItem;
import g2.g2_gp_project.entity.Product;
import g2.g2_gp_project.mongo.MongoTransaction;
import g2.g2_gp_project.mongo.MongoTransactionRepository;
import g2.g2_gp_project.repository.CustomerRepository;
import g2.g2_gp_project.repository.OrderItemRepository;
import g2.g2_gp_project.repository.OrderRepository;
import g2.g2_gp_project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MongoPostgresDataSync {

    private final MongoTransactionRepository mongoTransactionRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final int BATCH_SIZE = 200; // Process orders in smaller batches for better performance
    private static final int MONGO_PAGE_SIZE = 5000; // Smaller page size from MongoDB

    /**
     * Sync data from MongoDB to PostgreSQL
     * This method deletes all existing data in order_items, orders, customers, and products tables,
     * then syncs all data from MongoDB.
     */
    @Transactional
    public void syncData() {
        log.info("Starting MongoDB to PostgreSQL data sync...");
        long startTime = System.currentTimeMillis();
        try {
            // Step 1: Delete all existing data from the 4 tables (order_items, orders, customers, products)
            log.info("Deleting all existing data from order_items, orders, customers, and products tables...");
            deleteAllData();
            log.info("All existing data deleted successfully");

            // Step 2: Initialize empty caches (all data is fresh, no need to check existing)
            Map<String, Customer> customerCache = new HashMap<>();
            Map<String, Product> productCache = new HashMap<>();

            // Process MongoDB transactions in batches to avoid loading all at once
            int page = 0;
            int totalProcessed = 0;
            int totalOrders = 0;
            Map<String, List<MongoTransaction>> transactionGroups = new HashMap<>();

            log.info("Starting to load transactions from MongoDB collection 'transactions'...");
            while (true) {
                long pageStartTime = System.currentTimeMillis();
                Pageable pageable = PageRequest.of(page, MONGO_PAGE_SIZE);
                Page<MongoTransaction> transactionPage = mongoTransactionRepository.findAll(pageable);
                List<MongoTransaction> transactions = transactionPage.getContent();
                
                if (transactions.isEmpty()) {
                    log.info("No more transactions to load from MongoDB at page {}", page);
                    break;
                }
                
                log.info("Loaded {} transactions from MongoDB page {}", transactions.size(), page);

                // Group transactions by InvoiceNo
                for (MongoTransaction tx : transactions) {
                    if (tx.getInvoiceNo() != null) {
                        transactionGroups.computeIfAbsent(tx.getInvoiceNo(), k -> new ArrayList<>()).add(tx);
                    } else {
                        log.warn("Skipping transaction with null InvoiceNo: {}", tx.getId());
                    }
                }

                totalProcessed += transactions.size();
                long pageTime = (System.currentTimeMillis() - pageStartTime) / 1000;
                log.info("Loaded page {}: {} transactions in {}s (total loaded: {}, grouped into {} orders)", 
                        page, transactions.size(), pageTime, totalProcessed, transactionGroups.size());

                // Process in batches when we have enough groups
                while (transactionGroups.size() >= BATCH_SIZE) {
                    long batchStartTime = System.currentTimeMillis();
                    
                    // Extract batch (take first BATCH_SIZE items)
                    Map<String, List<MongoTransaction>> batch = new HashMap<>();
                    int count = 0;
                    var iterator = transactionGroups.entrySet().iterator();
                    while (iterator.hasNext() && count < BATCH_SIZE) {
                        var entry = iterator.next();
                        batch.put(entry.getKey(), entry.getValue());
                        iterator.remove(); // Remove from transactionGroups while iterating
                        count++;
                    }
                    
                    processBatch(batch, customerCache, productCache);
                    totalOrders += batch.size();
                    long batchTime = (System.currentTimeMillis() - batchStartTime) / 1000;
                    log.info("Processed batch: {} orders in {}s (total orders processed: {})", 
                            batch.size(), batchTime, totalOrders);
                }

                if (!transactionPage.hasNext()) {
                    break;
                }
                page++;
            }

            // Process remaining groups
            if (!transactionGroups.isEmpty()) {
                log.info("Processing final batch with {} orders", transactionGroups.size());
                processBatch(transactionGroups, customerCache, productCache);
                totalOrders += transactionGroups.size();
            }

            long duration = (System.currentTimeMillis() - startTime) / 1000;
            log.info("MongoDB to PostgreSQL sync completed successfully in {} seconds. Total: {} transactions processed, {} orders created", 
                    duration, totalProcessed, totalOrders);
            
            // Verify final counts
            long finalOrderCount = orderRepository.count();
            long finalCustomerCount = customerRepository.count();
            long finalProductCount = productRepository.count();
            long finalOrderItemCount = orderItemRepository.count();
            log.info("Final database counts - Orders: {}, Customers: {}, Products: {}, OrderItems: {}", 
                    finalOrderCount, finalCustomerCount, finalProductCount, finalOrderItemCount);
        } catch (Exception e) {
            log.error("Error during MongoDB to PostgreSQL sync", e);
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteAllData() {
        long deleteStartTime = System.currentTimeMillis();
        
        // Delete in correct order to respect foreign key constraints using native SQL for bulk delete
        // This is much faster than deleteAll() which deletes one by one
        
        log.info("Starting bulk delete of all data from 4 tables...");
        
        // 1. Delete all order_items (references orders and products)
        long countOrderItems = orderItemRepository.count();
        log.info("Deleting {} order items...", countOrderItems);
        int deletedItems = entityManager.createNativeQuery("DELETE FROM order_items").executeUpdate();
        log.info("Deleted {} order items", deletedItems);
        
        // 2. Delete all orders (references customers)
        long countOrders = orderRepository.count();
        log.info("Deleting {} orders...", countOrders);
        int deletedOrders = entityManager.createNativeQuery("DELETE FROM orders").executeUpdate();
        log.info("Deleted {} orders", deletedOrders);
        
        // 3. Delete all customers
        long countCustomers = customerRepository.count();
        log.info("Deleting {} customers...", countCustomers);
        int deletedCustomers = entityManager.createNativeQuery("DELETE FROM customers").executeUpdate();
        log.info("Deleted {} customers", deletedCustomers);
        
        // 4. Delete all products
        long countProducts = productRepository.count();
        log.info("Deleting {} products...", countProducts);
        int deletedProducts = entityManager.createNativeQuery("DELETE FROM products").executeUpdate();
        log.info("Deleted {} products", deletedProducts);
        
        long deleteTime = (System.currentTimeMillis() - deleteStartTime) / 1000;
        log.info("Bulk delete completed in {} seconds ({} order items, {} orders, {} customers, {} products)", 
                deleteTime, deletedItems, deletedOrders, deletedCustomers, deletedProducts);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void processBatch(Map<String, List<MongoTransaction>> transactionGroups, 
                             Map<String, Customer> customerCache,
                             Map<String, Product> productCache) {
        List<Order> ordersToSave = new ArrayList<>();
        List<OrderItem> itemsToSave = new ArrayList<>();
        List<Customer> customersToSave = new ArrayList<>();
        List<Product> productsToSave = new ArrayList<>();

        for (Map.Entry<String, List<MongoTransaction>> entry : transactionGroups.entrySet()) {
            String invoiceNo = entry.getKey(); // InvoiceNo → order_id
            List<MongoTransaction> items = entry.getValue();
            if (items.isEmpty()) continue;

            MongoTransaction first = items.get(0);

            // Create customer (no check - always create new from MongoDB data)
            if (first.getCustomerId() == null || first.getCustomerId().trim().isEmpty()) {
                log.warn("Skipping order {} - CustomerID is null or empty", invoiceNo);
                continue;
            }
            
            Customer customer = customerCache.get(first.getCustomerId());
            if (customer == null) {
                customer = new Customer();
                customer.setCustomerId(first.getCustomerId()); // CustomerID → customer_id
                customer.setCustomerName("Customer " + first.getCustomerId()); // customer_name = "Customer " + customer_id
                customer.setCountry(first.getCountry() != null ? first.getCountry() : ""); // Country → country
                customersToSave.add(customer);
                customerCache.put(first.getCustomerId(), customer);
            }

            // Create order - InvoiceNo → order_id
            Order order = new Order();
            order.setOrderId(invoiceNo); // InvoiceNo → order_id
            order.setCustomer(customer);
            order.setStatus("Completed");
            
            // InvoiceDate → order_date and created_at
            LocalDateTime invoiceDateTime = parseInvoiceDate(first.getInvoiceDate(), invoiceNo);
            order.setOrderDate(invoiceDateTime); // InvoiceDate → order_date
            order.setCreatedAt(invoiceDateTime); // InvoiceDate → created_at

            // Calculate total and create order items
            BigDecimal total = BigDecimal.ZERO;
            for (MongoTransaction item : items) {
                // Validate required fields
                if (item.getStockCode() == null || item.getStockCode().trim().isEmpty()) {
                    log.warn("Skipping item in order {} - StockCode is null or empty", invoiceNo);
                    continue;
                }
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    log.warn("Skipping item {} in order {} - Quantity is null or invalid: {}", 
                            item.getStockCode(), invoiceNo, item.getQuantity());
                    continue;
                }
                if (item.getUnitPrice() == null || item.getUnitPrice() <= 0) {
                    log.warn("Skipping item {} in order {} - UnitPrice is null or invalid: {}", 
                            item.getStockCode(), invoiceNo, item.getUnitPrice());
                    continue;
                }
                
                // Create product (no check - always create new from MongoDB data)
                Product product = productCache.get(item.getStockCode());
                if (product == null) {
                    product = new Product();
                    product.setStockCode(item.getStockCode()); // StockCode → stock_code (product_id)
                    product.setDescription(item.getDescription() != null ? item.getDescription() : ""); // Description → description
                    product.setUnitPrice(BigDecimal.valueOf(item.getUnitPrice())); // UnitPrice → unit_price
                    productsToSave.add(product);
                    productCache.put(item.getStockCode(), product);
                }

                // Create order item
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(product);
                orderItem.setQuantity(item.getQuantity()); // Quantity → quantity in order_item
                orderItem.setUnitPrice(BigDecimal.valueOf(item.getUnitPrice())); // UnitPrice → unit_price in order_item

                BigDecimal lineTotal = orderItem.getUnitPrice()
                        .multiply(BigDecimal.valueOf(orderItem.getQuantity()));
                total = total.add(lineTotal);
                itemsToSave.add(orderItem);
            }

            order.setSubtotal(total);
            order.setTax(BigDecimal.ZERO);
            order.setShippingFee(BigDecimal.ZERO);
            order.setTotalAmount(total);
            ordersToSave.add(order);
        }

        // Batch save all entities (save in correct order for foreign keys)
        if (!customersToSave.isEmpty()) {
            List<Customer> savedCustomers = customerRepository.saveAll(customersToSave);
            savedCustomers.forEach(c -> customerCache.put(c.getCustomerId(), c));
            entityManager.flush(); // Force flush to ensure data is persisted
            log.info("Saved {} new customers", savedCustomers.size());
        }

        if (!productsToSave.isEmpty()) {
            List<Product> savedProducts = productRepository.saveAll(productsToSave);
            savedProducts.forEach(p -> productCache.put(p.getStockCode(), p));
            entityManager.flush(); // Force flush to ensure data is persisted
            log.info("Saved {} new products", savedProducts.size());
        }

        if (!ordersToSave.isEmpty()) {
            List<Order> savedOrders = orderRepository.saveAll(ordersToSave);
            entityManager.flush(); // Force flush to ensure data is persisted
            log.info("Saved {} new orders", savedOrders.size());
        }

        if (!itemsToSave.isEmpty()) {
            List<OrderItem> savedItems = orderItemRepository.saveAll(itemsToSave);
            entityManager.flush(); // Force flush to ensure data is persisted
            log.info("Saved {} new order items", savedItems.size());
        }
        
        // Final flush and clear to free memory
        entityManager.clear();
    }

    private LocalDateTime parseInvoiceDate(String dateStr, String invoiceNo) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            log.warn("Invoice date is null or empty for invoice {}, using current date", invoiceNo);
            return LocalDateTime.now();
        }

        dateStr = dateStr.trim();
        
        // Try parsing as full datetime first (most common format in MongoDB)
        try {
            LocalDateTime result = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.debug("Successfully parsed date '{}' for invoice {} using format 'yyyy-MM-dd HH:mm:ss'", dateStr, invoiceNo);
            return result;
        } catch (Exception e) {
            // Try with milliseconds
            try {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            } catch (Exception e2) {
                // Try ISO format
                try {
                    return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception e3) {
                    // Try date only formats
                    String datePart = dateStr.contains(" ") ? dateStr.split(" ")[0] : dateStr;
                    try {
                        // Try MM/dd/yyyy
                        LocalDate parsedDate = LocalDate.parse(datePart, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                        return parsedDate.atStartOfDay();
                    } catch (Exception e4) {
                        try {
                            // Try dd/MM/yyyy
                            LocalDate parsedDate = LocalDate.parse(datePart, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            return parsedDate.atStartOfDay();
                        } catch (Exception e5) {
                            try {
                                // Try yyyy-MM-dd
                                LocalDate parsedDate = LocalDate.parse(datePart, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                                return parsedDate.atStartOfDay();
                            } catch (Exception e6) {
                                log.warn("Could not parse date '{}' for invoice {} - tried multiple formats. Using current date", 
                                        dateStr, invoiceNo);
                                return LocalDateTime.now();
                            }
                        }
                    }
                }
            }
        }
    }
}