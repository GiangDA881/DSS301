
package g2.g2_gp_project.mongo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MongoConnectionTest implements CommandLineRunner {

    @Autowired
    private MongoInvoiceRepository mongoInvoiceRepository;

    @Override
    public void run(String... args) {
        try {
            long count = mongoInvoiceRepository.count();
            System.out.println("MongoDB connection successful. Invoice count: " + count);
        } catch (Exception e) {
            System.err.println("MongoDB connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
