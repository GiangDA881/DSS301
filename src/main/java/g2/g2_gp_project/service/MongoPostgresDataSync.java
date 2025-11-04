package g2.g2_gp_project.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import g2.g2_gp_project.entity.Customer;
import g2.g2_gp_project.entity.Order;
import g2.g2_gp_project.entity.OrderItem;
import g2.g2_gp_project.entity.Product;
import g2.g2_gp_project.mongo.MongoInvoice;
import g2.g2_gp_project.mongo.MongoInvoiceRepository;
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

    private final MongoInvoiceRepository mongoInvoiceRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Value("${app.sync-on-startup:true}")
    private boolean syncOnStartupEnabled;

    @Value("${app.fail-on-sync-error:true}")
    private boolean failOnSyncError;

    @EventListener(ContextRefreshedEvent.class)
    @Transactional
    public void syncOnStartup() {
        if (!syncOnStartupEnabled) {
            log.info("Mongo-Postgres data sync on startup is disabled (app.sync-on-startup=false)");
            return;
        }
        try {
            syncData();
        } catch (Exception e) {
            log.error("Error during MongoDB to PostgreSQL sync on startup", e);
            if (failOnSyncError) {
                throw e;
            } else {
                log.warn("Continuing application startup despite sync error (app.fail-on-sync-error=false)");
            }
        }
    }

    @Transactional
    public void syncData() {
        log.info("Starting MongoDB to PostgreSQL data sync...");
        try {
            // Get all invoices from MongoDB
            List<MongoInvoice> invoices = mongoInvoiceRepository.findAll();
            log.info("Found {} invoices in MongoDB", invoices.size());

            // Group invoices by InvoiceNo to create orders
            Map<String, List<MongoInvoice>> invoiceGroups = new HashMap<>();
            for (MongoInvoice inv : invoices) {
                invoiceGroups.computeIfAbsent(inv.getInvoiceNo(), k -> new ArrayList<>()).add(inv);
            }

            // Process each invoice group into an order
            for (Map.Entry<String, List<MongoInvoice>> entry : invoiceGroups.entrySet()) {
                String invoiceNo = entry.getKey();
                List<MongoInvoice> items = entry.getValue();
                if (items.isEmpty()) continue;

                // Skip if order already exists
                if (orderRepository.findById(invoiceNo).isPresent()) {
                    log.debug("Order {} already exists, skipping", invoiceNo);
                    continue;
                }

                MongoInvoice first = items.get(0);

                // Find or create customer
                Customer customer = findOrCreateCustomer(first.getCustomerId(), first.getCountry());

                // Create order
                Order order = new Order();
                order.setOrderId(invoiceNo);
                order.setCustomer(customer);
                order.setStatus("Completed"); // Historical data is considered completed
                
                // Parse invoice date into LocalDateTime (keep time if present)
                String dateStr = first.getInvoiceDate();
                if (dateStr == null || dateStr.isBlank()) {
                    log.error("InvoiceDate is null/blank for invoice {} - skipping order", invoiceNo);
                    continue;
                }

                LocalDateTime orderDateTime = null;
                // Try common date-time patterns from the dataset
                DateTimeFormatter[] patterns = new DateTimeFormatter[] {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                    DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd")
                };

                // If the string contains a time part, try full patterns first
                boolean parsed = false;
                for (DateTimeFormatter fmt : patterns) {
                    try {
                        if (fmt.toString().contains("H") || fmt.toString().contains("h")) {
                            // pattern expects time
                            orderDateTime = LocalDateTime.parse(dateStr, fmt);
                        } else {
                            // pattern with only date -> parse date then startOfDay
                            java.time.LocalDate d = java.time.LocalDate.parse(dateStr.split(" ")[0], fmt);
                            orderDateTime = d.atStartOfDay();
                        }
                        parsed = true;
                        break;
                    } catch (Exception ex) {
                        // try next
                    }
                }

                if (!parsed) {
                    // Try to extract date and time separately if the string contains both
                    try {
                        String[] parts = dateStr.split(" ");
                        if (parts.length >= 2) {
                            String datePart = parts[0];
                            String timePart = parts[1];
                            // try yyyy-MM-dd + HH:mm[:ss]
                            try {
                                orderDateTime = LocalDateTime.parse(datePart + " " + timePart,
                                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                parsed = true;
                            } catch (Exception ex2) {
                                try {
                                    orderDateTime = LocalDateTime.parse(datePart + " " + timePart,
                                            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"));
                                    parsed = true;
                                } catch (Exception ex3) {
                                    // fallthrough
                                }
                            }
                        }
                    } catch (Exception ex) {
                        // ignore
                    }
                }

                if (!parsed || orderDateTime == null) {
                    log.error("Failed to parse InvoiceDate '{}' for invoice {} - skipping order", dateStr, invoiceNo);
                    continue;
                }

                order.setOrderDate(orderDateTime);
                order.setCreatedAt(orderDateTime);

                // Calculate total
                BigDecimal total = BigDecimal.ZERO;

                // Save order first to get ID for items
                order = orderRepository.save(order);

                // Process each line item
                for (MongoInvoice item : items) {
                    // Find or create product
                    Product product = findOrCreateProduct(item);

                    // Create order item
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setProduct(product);
                    orderItem.setQuantity(item.getQuantity());
                    orderItem.setUnitPrice(BigDecimal.valueOf(item.getUnitPrice()));

                    // Add to total
                    BigDecimal lineTotal = orderItem.getUnitPrice()
                        .multiply(BigDecimal.valueOf(orderItem.getQuantity()));
                    total = total.add(lineTotal);

                    // Save item
                    orderItemRepository.save(orderItem);
                }

                // Update order with total
                order.setSubtotal(total);
                order.setTax(BigDecimal.ZERO); // Historical data might not have tax
                order.setShippingFee(BigDecimal.ZERO);
                order.setTotalAmount(total);
                orderRepository.save(order);

                log.debug("Created order {} with {} items", order.getOrderId(), items.size());
            }

            log.info("MongoDB to PostgreSQL sync completed successfully");
        } catch (Exception e) {
            log.error("Error during MongoDB to PostgreSQL sync", e);
            throw e;
        }
    }

    private Customer findOrCreateCustomer(String customerId, String country) {
        return customerRepository.findById(customerId)
                .orElseGet(() -> {
                    Customer c = new Customer();
                    c.setCustomerId(customerId);
                    c.setCustomerName("Customer " + customerId); // Default name if not available
                    c.setCountry(country);
                    return customerRepository.save(c);
                });
    }

    private Product findOrCreateProduct(MongoInvoice item) {
        return productRepository.findById(item.getStockCode())
                .orElseGet(() -> {
                    Product p = new Product();
                    p.setStockCode(item.getStockCode());
                    p.setDescription(item.getDescription());
                    p.setUnitPrice(BigDecimal.valueOf(item.getUnitPrice()));
                    return productRepository.save(p);
                });
    }
}