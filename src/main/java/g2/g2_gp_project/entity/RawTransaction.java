package g2.g2_gp_project.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "retail_db")
public class RawTransaction {
    @Id
    private String id;

    @Field("InvoiceNo")
    private String invoiceNo;
    
    @Field("StockCode")
    private String stockCode;
    
    @Field("Description")
    private String description;
    
    @Field("Quantity")
    private String quantity;
    
    @Field("InvoiceDate")
    private String invoiceDate;
    
    @Field("UnitPrice")
    private String unitPrice;
    
    @Field("CustomerID")
    private String customerId;
    
    @Field("Country")
    private String country;

    private LocalDateTime importedAt;
    private String fileName;
    private Integer rowNumber;
}

