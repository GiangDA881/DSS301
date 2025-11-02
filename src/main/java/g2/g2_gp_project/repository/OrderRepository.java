// java
package g2.g2_gp_project.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import g2.g2_gp_project.dto.OrderSummaryResponse;
import g2.g2_gp_project.entity.Order;

public interface OrderRepository extends JpaRepository<Order, String> {
    @Query("SELECT new g2.g2_gp_project.dto.OrderSummaryResponse(o.orderId, o.customerIdRaw, c.customerName, o.orderDate, o.totalAmount, o.status) " +
            "FROM Order o JOIN o.customer c ORDER BY o.orderDate DESC")
    List<OrderSummaryResponse> findAllSummaries();

    @Query("SELECT new g2.g2_gp_project.dto.OrderSummaryResponse(o.orderId, o.customerIdRaw, c.customerName, o.orderDate, o.totalAmount, o.status) " +
        "FROM Order o JOIN o.customer c ORDER BY o.orderDate DESC")
    org.springframework.data.domain.Page<OrderSummaryResponse> findAllSummariesPaged(org.springframework.data.domain.Pageable pageable);
}