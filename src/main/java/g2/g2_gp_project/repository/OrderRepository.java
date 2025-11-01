// java
package g2.g2_gp_project.repository;

import g2.g2_gp_project.dto.OrderSummaryResponse;
import g2.g2_gp_project.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    @Query("SELECT new g2.g2_gp_project.dto.OrderSummaryResponse(o.orderId, c.customerName, o.orderDate, o.totalAmount, o.status) " +
            "FROM Order o JOIN o.customer c ORDER BY o.orderDate DESC")
    List<OrderSummaryResponse> findAllSummaries();
}