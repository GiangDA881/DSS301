package g2.g2_gp_project.mongo;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class MongoOrder {
    @Id
    private String orderId;
    // some Mongo documents may store only a customerId (reference) instead of a customerName.
    // Keep both fields so we can display a readable name when available and fall back to id.
    @Field("CustomerName")
    private String customerName;
    @Field("CustomerID")
    private String customerId;
    @Field("OrderDate")
    private java.time.LocalDateTime orderDate;
    @Field("TotalAmount")
    private BigDecimal totalAmount;
    @Field("Status")
    private String status;
    @Field("Items")
    private List<MongoOrderItem> items;
}