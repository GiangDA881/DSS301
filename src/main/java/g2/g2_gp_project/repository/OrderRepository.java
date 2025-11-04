package g2.g2_gp_project.repository;

import g2.g2_gp_project.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    Optional<Order> findByOrderId(String orderId);
    List<Order> findByCustomerId(String customerId);
    List<Order> findByStatus(String status);
    List<Order> findByOrderDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate = :date")
    Long countOrdersByDate(@Param("date") LocalDate date);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.orderDate BETWEEN :start AND :end")
    Double getTotalRevenueByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT MIN(o.orderDate) FROM Order o")
    LocalDate findFirstOrderDate();

    @Query("SELECT MAX(o.orderDate) FROM Order o")
    LocalDate findLastOrderDate();
}
