package g2.g2_gp_project.repository;

import g2.g2_gp_project.entity.RetailTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.data.mongodb.repository.Query;

@Repository
public interface RetailTransactionRepository extends MongoRepository<RetailTransaction, String> {

    // Lấy theo quốc gia (tùy chọn)
    List<RetailTransaction> findByCountry(String country);

    // Lấy theo CustomerID
    @Query("{ 'CustomerID': ?0 }")
    List<RetailTransaction> findByCustomerID(String customerID);

    // Nếu CustomerID lưu dưới dạng số
    @Query("{ 'CustomerID': ?0 }")
    List<RetailTransaction> findByCustomerIDAsNumber(double customerID);

    // ✅ Kiểm tra tồn tại CustomerID
    boolean existsByCustomerID(String customerID);
}
