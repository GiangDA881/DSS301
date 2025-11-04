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
@Table(name = "demand_forecasts")
public class DemandForecast {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "forecast_id")
    private Long forecastId;

    @Column(length = 100)
    private String category;

    @Column(name = "forecast_date")
    private LocalDate forecastDate;

    @Column(name = "actual_sales", precision = 12, scale = 2)
    private BigDecimal actualSales;

    @Column(name = "predicted_sales", precision = 12, scale = 2)
    private BigDecimal predictedSales;

    @Column(name = "forecast_model", length = 50)
    private String forecastModel;
}