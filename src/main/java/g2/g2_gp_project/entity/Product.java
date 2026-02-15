package g2.g2_gp_project.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {
    @Id
    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;
}