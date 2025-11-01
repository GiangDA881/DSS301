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
@Table(name = "campaign_proposals")
public class CampaignProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "campaign_name", length = 100)
    private String campaignName;

    @Column(name = "target_segment", length = 50)
    private String targetSegment;

    @Column(name = "expected_cost", precision = 12, scale = 2)
    private BigDecimal expectedCost;

    @Column(name = "expected_revenue", precision = 12, scale = 2)
    private BigDecimal expectedRevenue;

    @Column(precision = 5, scale = 2)
    private BigDecimal roi;

    @Column(name = "created_at")
    private LocalDate createdAt;
}