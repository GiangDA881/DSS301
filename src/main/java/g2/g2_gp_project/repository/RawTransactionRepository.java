package g2.g2_gp_project.repository;

import g2.g2_gp_project.entity.RawTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawTransactionRepository extends MongoRepository<RawTransaction, String> {
    
    /**
     * Tìm kiếm dữ liệu theo description với hỗ trợ phân trang
     */
    Page<RawTransaction> findByDescriptionContaining(String keyword, Pageable pageable);
}

