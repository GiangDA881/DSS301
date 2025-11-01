package g2.g2_gp_project.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "invoices")
public class MongoInvoice {
    @Id
    private String id;
    @Field("InvoiceNo")
    private String invoiceNo;
    @Field("StockCode")
    private String stockCode;
    @Field("Description")
    private String description;
    @Field("Quantity")
    private Integer quantity;
    @Field("InvoiceDate")
    private String invoiceDate;
    @Field("UnitPrice")
    private Double unitPrice;
    @Field("CustomerID")
    private String customerId;
    @Field("Country")
    private String country;
}