package g2.g2_gp_project.repository;

import g2.g2_gp_project.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(String orderId);
    List<OrderItem> findByProductId(String productId);

    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.productId = productId")
    Integer getTotalQuantityByProduct(@Param("productId") String productId);}

