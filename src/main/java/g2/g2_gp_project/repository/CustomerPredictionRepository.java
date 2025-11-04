package g2.g2_gp_project.repository;

import g2.g2_gp_project.dto.RfmResultDTO;
import g2.g2_gp_project.entity.CustomerPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerPredictionRepository extends JpaRepository<CustomerPrediction, Integer> {
    List<CustomerPrediction> findBySegment(String segment);
    
    /**
     * Lấy danh sách các segment có sẵn trong bảng
     * @return Danh sách segment duy nhất
     */
    @Query(value = "SELECT DISTINCT segment FROM customer_predictions WHERE segment IS NOT NULL ORDER BY segment", 
           nativeQuery = true)
    List<String> findDistinctSegments();
    
    /**
     * Tính toán RFM từ dữ liệu Orders và Order_Items
     * @return Danh sách RFM cho từng khách hàng
     */
    @Query(value = """
        SELECT 
            o.customer_id AS customerId,
            MAX(o.order_date) AS lastPurchaseDate,
            COUNT(DISTINCT o.order_id) AS frequency,
            SUM(oi.quantity * oi.unit_price) AS monetary
        FROM orders o
        INNER JOIN order_items oi ON o.order_id = oi.order_id
        WHERE o.status != 'Cancelled'
        GROUP BY o.customer_id
        HAVING SUM(oi.quantity * oi.unit_price) > 0
        ORDER BY monetary DESC
        """, nativeQuery = true)
    List<RfmResultDTO> calculateRfm();
}