package g2.g2_gp_project.repository;



import g2.g2_gp_project.entity.RetailData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RetailDataRepository extends MongoRepository<RetailData, String> {
    List<RetailData> findByCountry(String country);
    List<RetailData> findByInvoiceDateBetween(LocalDateTime start, LocalDateTime end);
    List<RetailData> findByCustomerID(String customerId);
}
