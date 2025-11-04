package g2.g2_gp_project.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Entity
@Table(name = "campaign_proposals")
@Data
public class CampaignProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer campaignId;
    
    private String campaignName;
    private String targetSegment;
    private Double expectedCost;
    private Double expectedRevenue;
    private Double roi;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
}
