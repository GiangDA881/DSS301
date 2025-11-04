package g2.g2_gp_project.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoTransactionRepository extends MongoRepository<MongoTransaction, String> {
}