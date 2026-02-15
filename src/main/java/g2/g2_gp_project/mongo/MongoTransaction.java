package g2.g2_gp_project.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "transactions")
public class MongoTransaction {
    @Id
    private String id;
    @org.springframework.data.mongodb.core.mapping.Field("InvoiceNo")
    private String invoiceNo;
    @org.springframework.data.mongodb.core.mapping.Field("StockCode")
    private String stockCode;
    @org.springframework.data.mongodb.core.mapping.Field("Description")
    private String description;
    @org.springframework.data.mongodb.core.mapping.Field("Quantity")
    private Integer quantity;
    @org.springframework.data.mongodb.core.mapping.Field("InvoiceDate")
    private String invoiceDate;
    @org.springframework.data.mongodb.core.mapping.Field("UnitPrice")
    private Double unitPrice;
    @org.springframework.data.mongodb.core.mapping.Field("CustomerID")
    private String customerId;
    @org.springframework.data.mongodb.core.mapping.Field("Country")
    private String country;
}