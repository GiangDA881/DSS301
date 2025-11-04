package g2.g2_gp_project.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "customer_predictions")
@Data
public class CustomerPrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "customer_id")
    private String customerId;
    
    @Column(name = "segment")
    private String segment;
    
    @Column(name = "frequency")
    private Integer frequency;
    
    @Column(name = "monetary")
    private Double monetary;
    
    @Column(name = "recency")
    private Integer recency;
    
    @Column(name = "repurchase_probability")
    private Double repurchaseProbability;
    
    @Column(name = "prediction_date")
    private java.util.Date predictionDate;
}
