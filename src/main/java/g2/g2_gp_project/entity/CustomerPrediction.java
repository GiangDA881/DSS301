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
@Table(name = "customer_predictions")
public class CustomerPrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private Integer recency;
    private Integer frequency;

    @Column(precision = 12, scale = 2)
    private BigDecimal monetary;

    @Column(name = "repurchase_probability", precision = 5, scale = 2)
    private BigDecimal repurchaseProbability;

    @Column(name = "prediction_date")
    private LocalDate predictionDate;

    @Column(length = 50)
    private String segment;
}