package g2.g2_gp_project.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import g2.g2_gp_project.dto.OrderDetailResponse;
import g2.g2_gp_project.dto.OrderItemResponse;
import g2.g2_gp_project.dto.OrderSummaryResponse;
import g2.g2_gp_project.entity.Order;
import g2.g2_gp_project.entity.OrderItem;
import g2.g2_gp_project.repository.OrderItemRepository;
import g2.g2_gp_project.repository.OrderRepository;
import g2.g2_gp_project.service.OrderService;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderService orderService;

    public OrderController(OrderRepository orderRepository, OrderItemRepository orderItemRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderService = orderService;
    }

    // List with optional search params:
    // q = orderId or customer name (partial, case-insensitive)
    // status = order status (use "All" or empty to skip)
    // orderDate = yyyy-MM-dd
    @GetMapping
    public List<OrderSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderDate
    ) {
        // load summaries from Postgres (DTO projection)
        List<OrderSummaryResponse> list = orderRepository.findAllSummaries();

        // filter q
        if (q != null && !q.trim().isEmpty()) {
            String qq = q.trim().toLowerCase();
            list = list.stream()
                    .filter(o -> (o.getOrderId() != null && o.getOrderId().toLowerCase().contains(qq)) ||
                            (o.getCustomerName() != null && o.getCustomerName().toLowerCase().contains(qq)))
                    .collect(Collectors.toList());
        }

        // filter status
        if (status != null && !status.trim().isEmpty() && !"All".equalsIgnoreCase(status.trim())) {
            String st = status.trim().toLowerCase();
            list = list.stream()
                    .filter(o -> o.getStatus() != null && o.getStatus().toLowerCase().equals(st))
                    .collect(Collectors.toList());
        }

        // filter orderDate (exact match)
        if (orderDate != null && !orderDate.trim().isEmpty()) {
            try {
                LocalDate d = LocalDate.parse(orderDate.trim());
                list = list.stream()
                        .filter(o -> o.getOrderDate() != null && o.getOrderDate().isEqual(d))
                        .collect(Collectors.toList());
            } catch (DateTimeParseException e) {
                // ignore invalid date format -> no date filtering
            }
        }

        return list;
    }

    // Debug endpoint: quick sanity check for orders in Postgres
    @GetMapping("/debug")
    public ResponseEntity<?> debug() {
        long count = orderRepository.count();
        Optional<Order> sample = orderRepository.findAll().stream().findFirst();
        return ResponseEntity.ok(Map.of(
                "count", count,
                "sample", sample.orElse(null)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailResponse> detail(@PathVariable String id) {
        Optional<Order> opt = orderRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Order o = opt.get();

        List<OrderItem> items = orderItemRepository.findByOrderOrderId(id);
        List<OrderItemResponse> itemResponses = items.stream().map(it -> {
            BigDecimal line = BigDecimal.ZERO;
            if (it.getUnitPrice() != null && it.getQuantity() != null) {
                line = it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity()));
            }
            return new OrderItemResponse(
                    it.getItemId(),
                    it.getProduct() != null ? it.getProduct().getStockCode() : null,
                    it.getProduct() != null ? it.getProduct().getDescription() : null,
                    it.getQuantity(),
                    it.getUnitPrice(),
                    line
            );
        }).collect(Collectors.toList());

        OrderDetailResponse detail = new OrderDetailResponse(
                o.getOrderId(),
                o.getCustomer() != null ? o.getCustomer().getCustomerName() : null,
                o.getOrderDate(),
                o.getTotalAmount(),
                o.getStatus(),
                itemResponses
        );

        return ResponseEntity.ok(detail);
    }

    // Create a new order
    @org.springframework.web.bind.annotation.PostMapping
    public ResponseEntity<?> createOrder(@org.springframework.web.bind.annotation.RequestBody g2.g2_gp_project.dto.CreateOrderRequest req) {
        if (orderService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Order service unavailable"));
        }
        Order created = orderService.createOrder(req);
        return ResponseEntity.ok(Map.of(
                "orderId", created.getOrderId(),
                "totalAmount", created.getTotalAmount()
        ));
    }
}