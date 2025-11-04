package g2.g2_gp_project.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    @Column(name = "total_revenue", precision = 12, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "ranking")
    private Integer ranking;

    @Column(name = "analysis_date")
    private LocalDate analysisDate;
}
