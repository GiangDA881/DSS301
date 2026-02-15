package g2.g2_gp_project.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MongoInvoiceRepository extends MongoRepository<MongoInvoice, String> {
    List<MongoInvoice> findByInvoiceNo(String invoiceNo);
    List<MongoInvoice> findByCustomerId(String customerId);
    List<MongoInvoice> findByStockCode(String stockCode);
}