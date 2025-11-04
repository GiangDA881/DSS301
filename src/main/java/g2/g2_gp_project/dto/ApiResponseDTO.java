package g2.g2_gp_project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDTO {
    @JsonProperty("recommended_plan")
    private RecommendedPlan recommendedPlan;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedPlan {
        @JsonProperty("predicted_roi")
        private double predictedRoi;
        
        @JsonProperty("total_cost")
        private double totalCost;
        
        @JsonProperty("expected_retention")
        private int expectedRetention;
        
        @JsonProperty("revenue_saved")
        private double revenueSaved;
        
        @JsonProperty("distribution")
        private List<ActionDistribution> distribution;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionDistribution {
        @JsonProperty("action_id")
        private String actionId;
        
        @JsonProperty("action_name")
        private String actionName;
        
        @JsonProperty("assigned_count")
        private int assignedCount;
        
        @JsonProperty("action_cost")
        private double actionCost;
    }
}
