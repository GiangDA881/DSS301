package g2.g2_gp_project.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MongoOrderRepository extends MongoRepository<MongoOrder, String> {
    List<MongoOrder> findByCustomerNameContainingIgnoreCase(String customerName);
    List<MongoOrder> findByStatus(String status);
}