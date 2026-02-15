package g2.g2_gp_project.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import g2.g2_gp_project.entity.Order;
import g2.g2_gp_project.entity.OrderItem;
import g2.g2_gp_project.mongo.MongoOrder;
import g2.g2_gp_project.mongo.MongoOrderItem;
import g2.g2_gp_project.mongo.MongoOrderRepository;
import g2.g2_gp_project.repository.OrderItemRepository;
import g2.g2_gp_project.repository.OrderRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderSyncService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MongoOrderRepository mongoOrderRepository;

    public void syncOrderToMongo(String orderId) {
        Optional<Order> opt = orderRepository.findById(orderId);
        if (opt.isEmpty()) return;
        Order o = opt.get();

        List<OrderItem> items = orderItemRepository.findByOrderOrderId(orderId);
        List<MongoOrderItem> itemDocs = items.stream().map(it -> {
            BigDecimal line = BigDecimal.ZERO;
            if (it.getUnitPrice() != null && it.getQuantity() != null) {
                line = it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity()));
            }
            return new MongoOrderItem(
                    it.getItemId(),
                    it.getProduct() != null ? it.getProduct().getStockCode() : null,
                    it.getProduct() != null ? it.getProduct().getDescription() : null,
                    it.getQuantity(),
                    it.getUnitPrice(),
                    line
            );
        }).collect(Collectors.toList());

        MongoOrder mo = new MongoOrder(
                o.getOrderId(),
        o.getCustomer() != null ? o.getCustomer().getCustomerName() : null,
        o.getCustomer() != null ? o.getCustomer().getCustomerId() : null,
        o.getOrderDate(),
        o.getTotalAmount(),
        o.getStatus(),
        itemDocs
        );

        mongoOrderRepository.save(mo);
    }

    public void syncAllOrdersToMongo() {
        List<Order> all = orderRepository.findAll();
        all.forEach(o -> syncOrderToMongo(o.getOrderId()));
    }
}