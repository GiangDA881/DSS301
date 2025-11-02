package g2.g2_gp_project.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @EventListener(ContextRefreshedEvent.class)
    @Transactional
    public void syncOnStartup() {
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
                
                // Parse invoice date (assuming format like "MM/dd/yyyy HH:mm" or similar)
                try {
                    // Try different date formats
                    String dateStr = first.getInvoiceDate();
                    LocalDate orderDate = null;
                    
                    try {
                        // Try MM/dd/yyyy format
                        orderDate = LocalDate.parse(dateStr.split(" ")[0], 
                            DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                    } catch (Exception e) {
                        try {
                            // Try dd/MM/yyyy format
                            orderDate = LocalDate.parse(dateStr.split(" ")[0],
                                DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        } catch (Exception e2) {
                            // Default to current date if parsing fails
                            orderDate = LocalDate.now();
                            log.warn("Could not parse date '{}' for invoice {}, using current date", 
                                dateStr, invoiceNo);
                        }
                    }
                    order.setOrderDate(orderDate);
                } catch (Exception e) {
                    log.warn("Error parsing invoice date for {}: {}", invoiceNo, e.getMessage());
                    order.setOrderDate(LocalDate.now());
                }

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
            throw e; // Re-throw to fail startup if sync fails
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