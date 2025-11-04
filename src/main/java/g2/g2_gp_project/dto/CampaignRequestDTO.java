package g2.g2_gp_project.dto;

import lombok.Data;
import java.util.List;

@Data
public class CampaignRequestDTO {
    private String campaignName;
    private String targetSegment;
    private double totalBudget;
    private List<ApiRequestBody.AvailableAction> availableActions;
    private String optimizationGoal; // "ROI", "CPC", hoặc "CONVERSION"
}
