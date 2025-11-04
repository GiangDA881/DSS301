package g2.g2_gp_project.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;


    @JoinColumn(name = "order_id", nullable = false)
    private String order;


    @JoinColumn(name = "product_id", nullable = false)
    private String product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;
}
