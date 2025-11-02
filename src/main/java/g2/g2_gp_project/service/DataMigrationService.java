package g2.g2_gp_project.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import g2.g2_gp_project.entity.Customer;
import g2.g2_gp_project.entity.Order;
import g2.g2_gp_project.entity.OrderItem;
import g2.g2_gp_project.entity.Product;
import g2.g2_gp_project.repository.CustomerRepository;
import g2.g2_gp_project.repository.OrderItemRepository;
import g2.g2_gp_project.repository.OrderRepository;
import g2.g2_gp_project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataMigrationService {

    private final g2.g2_gp_project.mongo.MongoTransactionRepository mongoTransactionRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public void migrateData() {
        log.info("Starting data migration from MongoDB to PostgreSQL...");
        
        try {
            // Verify MongoDB connection
            if (mongoTransactionRepository == null) {
                throw new RuntimeException("MongoDB repository is not initialized");
            }

            int totalTx;
            int processedOrders = 0;
            int savedOrders = 0;

            // Get all transactions from MongoDB
            List<g2.g2_gp_project.mongo.MongoTransaction> txs = mongoTransactionRepository.findAll();
            totalTx = txs.size();
            log.info("Found {} transactions in MongoDB", totalTx);

            if (totalTx == 0) {
                log.warn("No transactions found in MongoDB. Please check MongoDB connection and data.");
                return;
            }

            // Group transactions by InvoiceNo
            Map<String, List<g2.g2_gp_project.mongo.MongoTransaction>> txGroups = new HashMap<>();
            for (g2.g2_gp_project.mongo.MongoTransaction tx : txs) {
                txGroups.computeIfAbsent(tx.getInvoiceNo(), k -> new ArrayList<>()).add(tx);
            }

            log.info("Processing {} unique orders", txGroups.size());

            for (Map.Entry<String, List<g2.g2_gp_project.mongo.MongoTransaction>> entry : txGroups.entrySet()) {
                String invoiceNo = entry.getKey();
                List<g2.g2_gp_project.mongo.MongoTransaction> items = entry.getValue();
                if (items.isEmpty()) continue;
                g2.g2_gp_project.mongo.MongoTransaction firstItem = items.get(0);
                Customer customer = createOrUpdateCustomerTx(firstItem);
                Order order = orderRepository.findById(invoiceNo).orElse(null);
                boolean isNewOrder = (order == null);
                if (isNewOrder) {
                    order = new Order();
                    order.setOrderId(invoiceNo);
                }
                order.setCustomer(customer);
                order.setStatus("Completed");
                order.setCustomerIdRaw(firstItem.getCustomerId());
                order.setCountry(firstItem.getCountry());
                String dateStr = firstItem.getInvoiceDate();
                if (dateStr != null) {
                    try {
                        LocalDateTime orderDateTime = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        order.setOrderDate(orderDateTime);
                        order.setCreatedAt(orderDateTime);
                    } catch (Exception e) {
                        log.error("Failed to parse InvoiceDate '{}' for invoice {}: {}", dateStr, invoiceNo, e.getMessage());
                        continue; // skip this order if date is invalid
                    }
                } else {
                    log.error("InvoiceDate is null for invoice {}", invoiceNo);
                    continue; // skip this order if date is missing
                }
                BigDecimal subtotal = BigDecimal.ZERO;
                List<OrderItem> orderItems = new ArrayList<>();
                // Remove all existing items for this order (to fully sync)
                if (!isNewOrder) {
                    List<OrderItem> existingItems = orderItemRepository.findAll();
                    for (OrderItem oi : existingItems) {
                        if (oi.getOrder().getOrderId().equals(order.getOrderId())) {
                            orderItemRepository.delete(oi);
                        }
                    }
                }
                for (g2.g2_gp_project.mongo.MongoTransaction item : items) {
                    Product product = createOrUpdateProductTx(item);
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setProduct(product);
                    orderItem.setQuantity(item.getQuantity());
                    orderItem.setUnitPrice(BigDecimal.valueOf(item.getUnitPrice()));
                    BigDecimal lineTotal = orderItem.getUnitPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
                    subtotal = subtotal.add(lineTotal);
                    orderItems.add(orderItem);
                }
                order.setSubtotal(subtotal);
                order.setTax(BigDecimal.ZERO);
                order.setShippingFee(BigDecimal.ZERO);
                order.setTotalAmount(subtotal);
                order = orderRepository.save(order);
                processedOrders++;
                for (OrderItem item : orderItems) {
                    orderItemRepository.save(item);
                }
                Order savedOrder = orderRepository.findById(order.getOrderId()).orElse(null);
                if (savedOrder == null) {
                    log.error("Failed to save order {}", order.getOrderId());
                    throw new RuntimeException("Order " + order.getOrderId() + " was not saved successfully");
                }
                savedOrders++;
                log.info((isNewOrder ? "Created" : "Updated") + " order {} with {} items, total: {}", order.getOrderId(), orderItems.size(), order.getTotalAmount());
            }
            log.info("Data migration completed: Found {} transactions, Processed {} orders, Successfully saved {} orders", totalTx, processedOrders, savedOrders);
        } catch (Exception e) {
            log.error("Error during data migration: {}", e.getMessage(), e);
            throw new RuntimeException("Data migration failed: " + e.getMessage(), e);
        }
    }

    private Customer createOrUpdateCustomerTx(g2.g2_gp_project.mongo.MongoTransaction tx) {
        String rawId = tx.getCustomerId();
        String customerId = rawId != null && rawId.contains(".") ? rawId.substring(0, rawId.indexOf('.')) : rawId;
        return customerRepository.findById(customerId)
                .orElseGet(() -> {
                    Customer customer = new Customer();
                    customer.setCustomerId(customerId);
                    customer.setCustomerName("Customer " + customerId);
                    customer.setCountry(tx.getCountry());
                    customer.setRegion("");
                    customer.setEmail("");
                    customer.setGender("");
                    customer.setAge(null);
                    return customerRepository.save(customer);
                });
    }

    private Product createOrUpdateProductTx(g2.g2_gp_project.mongo.MongoTransaction tx) {
        return productRepository.findById(tx.getStockCode())
                .orElseGet(() -> {
                    Product product = new Product();
                    product.setStockCode(tx.getStockCode());
                    product.setDescription(tx.getDescription());
                    product.setUnitPrice(BigDecimal.valueOf(tx.getUnitPrice()));
                    return productRepository.save(product);
                });
    }
}