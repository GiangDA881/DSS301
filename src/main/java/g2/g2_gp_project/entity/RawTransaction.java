package g2.g2_gp_project.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions_raw")
public class RawTransaction {
    @Id
    private String id;

    private String invoiceNo;
    private String stockCode;
    private String description;
    private String quantity;
    private String invoiceDate;
    private String unitPrice;
    private String customerId;
    private String country;

    private LocalDateTime importedAt;
    private String fileName;
    private Integer rowNumber;
}

