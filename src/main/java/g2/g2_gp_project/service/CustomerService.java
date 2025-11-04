package g2.g2_gp_project.service;

import g2.g2_gp_project.dto.CustomerSummary;
import g2.g2_gp_project.entity.RetailTransaction;
import g2.g2_gp_project.repository.RetailTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

@Service
public class CustomerService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RetailTransactionRepository repository;

    public List<CustomerSummary> getCustomerList(int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        int skip = (page - 1) * size;

        // Lọc trước: Quantity và UnitPrice phải là số
        Criteria validCriteria = new Criteria().andOperator(
                Criteria.where("Quantity").ne(null),
                Criteria.where("UnitPrice").ne(null),
                Criteria.where("Quantity").not().type(2), // loại bỏ string
                Criteria.where("UnitPrice").not().type(2)
        );

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(validCriteria),

                // Ép kiểu sang số (nếu có thể)
                Aggregation.project("CustomerID", "Country", "Quantity", "UnitPrice")
                        .and(ConvertOperators.ToDouble.toDouble("$Quantity")).as("quantityNum")
                        .and(ConvertOperators.ToDouble.toDouble("$UnitPrice")).as("priceNum"),

                Aggregation.group("CustomerID", "Country")
                        .sum(
                                ArithmeticOperators.Multiply.valueOf("$quantityNum").multiplyBy("$priceNum")
                        ).as("totalSpent")
                        .count().as("totalOrders"),

                Aggregation.project("totalSpent", "totalOrders")
                        .and("_id.CustomerID").as("customerID")
                        .and("_id.Country").as("country"),

                Aggregation.sort(Sort.by(Sort.Direction.DESC, "totalSpent")),
                Aggregation.skip(skip),
                Aggregation.limit(size)
        );

        return mongoTemplate.aggregate(aggregation, "online_retail", CustomerSummary.class).getMappedResults();
    }

    public List<RetailTransaction> getCustomerTransactions(String customerId) {
        try {
            return repository.findByCustomerIDAsNumber(Double.parseDouble(customerId));
        } catch (NumberFormatException e) {
            return repository.findByCustomerID(customerId);
        }
    }

}
