package g2.g2_gp_project.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import g2.g2_gp_project.dto.CreateOrderItemRequest;
import g2.g2_gp_project.dto.CreateOrderRequest;
import g2.g2_gp_project.entity.Customer;
import g2.g2_gp_project.entity.Order;
import g2.g2_gp_project.entity.OrderItem;
import g2.g2_gp_project.entity.OrderSequence;
import g2.g2_gp_project.entity.Product;
import g2.g2_gp_project.repository.CustomerRepository;
import g2.g2_gp_project.repository.OrderItemRepository;
import g2.g2_gp_project.repository.OrderRepository;
import g2.g2_gp_project.repository.OrderSequenceRepository;
import g2.g2_gp_project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderSequenceRepository orderSequenceRepository;

    @Transactional
    public synchronized String nextOrderId() {
        final String seqName = "orders";
        OrderSequence seq = orderSequenceRepository.findById(seqName).orElseGet(() -> {
            long start = orderRepository.count();
            OrderSequence s = new OrderSequence(seqName, start);
            return orderSequenceRepository.save(s);
        });
        seq.setLastValue(seq.getLastValue() + 1);
        orderSequenceRepository.save(seq);
        // produce a human-friendly numeric id with padding
        return String.format("%08d", seq.getLastValue());
    }

    private String generateCustomerId() {
        return "CUST-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String generateProductCode() {
        return "PRD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @Transactional
    public Order createOrder(CreateOrderRequest req) {
        // find or create customer by name
        String custName = req.getCustomerName() != null ? req.getCustomerName().trim() : "Unknown";
        Optional<Customer> oc = customerRepository.findByCustomerName(custName);
        Customer customer = oc.orElseGet(() -> {
            Customer c = new Customer();
            c.setCustomerId(generateCustomerId());
            c.setCustomerName(custName);
            return customerRepository.save(c);
        });

        Order order = new Order();
        order.setOrderId(nextOrderId());
        order.setCustomer(customer);
        if (req.getOrderDate() != null && !req.getOrderDate().isBlank()) {
            // Try to parse as LocalDateTime, fallback to LocalDate at midnight
            try {
                order.setOrderDate(java.time.LocalDateTime.parse(req.getOrderDate(), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (Exception e) {
                try {
                    order.setOrderDate(java.time.LocalDate.parse(req.getOrderDate(), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay());
                } catch (Exception ex) {
                    order.setOrderDate(java.time.LocalDateTime.now());
                }
            }
        } else {
            order.setOrderDate(java.time.LocalDateTime.now());
        }
        order.setStatus(req.getStatus());

        order = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;

        List<CreateOrderItemRequest> items = req.getItems();
        if (items != null) {
            for (CreateOrderItemRequest it : items) {
                Product product = null;
                if (it.getProductId() != null && !it.getProductId().isBlank()) {
                    product = productRepository.findById(it.getProductId()).orElse(null);
                }
                if (product == null) {
                    product = new Product();
                    product.setStockCode(generateProductCode());
                    product.setDescription(it.getDescription());
                    product.setUnitPrice(it.getUnitPrice() != null ? it.getUnitPrice() : BigDecimal.ZERO);
                    product = productRepository.save(product);
                }

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(product);
                orderItem.setQuantity(it.getQuantity() != null ? it.getQuantity() : 1);
                orderItem.setUnitPrice(it.getUnitPrice() != null ? it.getUnitPrice() : BigDecimal.ZERO);
                orderItem = orderItemRepository.save(orderItem);

                BigDecimal line = orderItem.getUnitPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
                total = total.add(line);
            }
        }

        // assign totals
        order.setSubtotal(total);
        order.setTax(BigDecimal.ZERO);
        order.setShippingFee(BigDecimal.ZERO);
        order.setTotalAmount(total);

        order = orderRepository.save(order);

        return order;
    }
}
