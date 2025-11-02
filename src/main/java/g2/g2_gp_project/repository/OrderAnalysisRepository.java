package g2.g2_gp_project.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderAnalysisRepository extends org.springframework.data.jpa.repository.JpaRepository<g2.g2_gp_project.entity.Order, String> {
    @Query(value = """
        SELECT p.stock_code AS stockCode,
               p.description AS description,
               c.country AS country,
               SUM(oi.quantity) AS totalQuantity,
               SUM(oi.quantity * oi.unit_price) AS totalRevenue
        FROM orders o
        JOIN order_items oi ON o.order_id = oi.order_id
        JOIN customers c ON o.customer_id = c.customer_id
        JOIN products p ON oi.product_id = p.stock_code
        WHERE DATE(o.order_date) >= CAST(:fromDate AS DATE)
        AND DATE(o.order_date) <= CAST(:toDate AS DATE)
        AND (:countries IS NULL OR c.country IN (:countries))
        AND oi.quantity > 0
        GROUP BY p.stock_code, p.description, c.country
        ORDER BY SUM(oi.quantity * oi.unit_price) DESC
        LIMIT :topN
    """, nativeQuery = true)
    List<Map<String, Object>> findTopProductsByCountry(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("countries") List<String> countries,
        @Param("topN") Integer topN
    );

    @Query(value = """
        SELECT DISTINCT c.country
        FROM customers c
        ORDER BY c.country
    """, nativeQuery = true)
    List<String> findAllCountries();

    @Query(value = """
        SELECT SUM(oi.quantity * oi.unit_price) as total
        FROM orders o
        JOIN order_items oi ON o.order_id = oi.order_id
        JOIN customers c ON o.customer_id = c.customer_id
        WHERE CAST(o.order_date AS DATE) BETWEEN :fromDate AND :toDate
        AND c.country = :country
        AND oi.quantity > 0
    """, nativeQuery = true)
    BigDecimal findTotalRevenueByCountry(
        @Param("country") String country,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );
}