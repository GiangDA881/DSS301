package g2.g2_gp_project.service;

import g2.g2_gp_project.entity.RawTransaction;
import g2.g2_gp_project.repository.RawTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MongoDataService {
    
    @Autowired
    private RawTransactionRepository rawTransactionRepository;
    
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Lấy dữ liệu thô từ MongoDB, hỗ trợ cả Tìm kiếm và Phân trang
     */
    public Page<RawTransaction> getMongoDataPaginated(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            // Nếu có từ khóa, gọi hàm tìm kiếm
            return rawTransactionRepository.findByDescriptionContaining(keyword, pageable);
        } else {
            // Nếu không, gọi hàm findAll (phân trang)
            return rawTransactionRepository.findAll(pageable);
        }
    }
    
    /**
     * Lấy sản phẩm theo stock code (tìm cả String và Number trong MongoDB)
     */
    public List<RawTransaction> getProductByStockCode(String stockCode) {
        // Tạo query tìm StockCode - xử lý cả String và Number
        Query query = new Query();
        
        Criteria criteria = new Criteria().orOperator(
            Criteria.where("StockCode").is(stockCode),  // Tìm dưới dạng String
            Criteria.where("StockCode").is(tryParseInt(stockCode))  // Tìm dưới dạng Number
        );
        
        query.addCriteria(criteria);
        
        return mongoTemplate.find(query, RawTransaction.class);
    }
    
    /**
     * Thử parse String thành Integer, trả về null nếu không thành công
     */
    private Integer tryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}


