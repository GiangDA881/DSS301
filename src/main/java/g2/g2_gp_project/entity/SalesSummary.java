package g2.g2_gp_project.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sales_summary")
public class SalesSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String country;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "stock_code", referencedColumnName = "stock_code")
    private Product product;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    @Column(name = "total_revenue", precision = 12, scale = 2)
    private BigDecimal totalRevenue;

    private Integer ranking;

    @Column(name = "analysis_date")
    private LocalDate analysisDate;
}