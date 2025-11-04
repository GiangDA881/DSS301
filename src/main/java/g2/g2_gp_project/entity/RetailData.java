package g2.g2_gp_project.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Getter@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "dss")
public class RetailData {
    @Id
    private String id; // ID tự động của MongoDB

    @Field("InvoiceNo")
    private String invoiceNo;

    @Field("StockCode")
    private String stockCode;

    @Field("Description")
    private String description;

    @Field("Quantity")
    private int quantity;

    @Field("InvoiceDate")
    private String  invoiceDate;

    @Field("UnitPrice")
    private double unitPrice;

    @Field("CustomerID")
    private String customerID;

    @Field("Country")
    private String country;

    public LocalDateTime getInvoiceDate() {
        if (invoiceDate == null) return null;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(invoiceDate, formatter);
        } catch (Exception e) {
            return null;
        }
    }
}
