package g2.g2_gp_project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiRequestBody {
    @JsonProperty("total_budget")
    private double totalBudget;
    
    @JsonProperty("target_audience")
    private List<AudienceMember> targetAudience;
    
    @JsonProperty("available_actions")
    private List<AvailableAction> availableActions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudienceMember {
        @JsonProperty("customer_id")
        private String customerId;
        
        @JsonProperty("avg_revenue")
        private double avgRevenue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailableAction {
        @JsonProperty("action_id")
        private String actionId;
        
        @JsonProperty("cost_per_user")
        private double costPerUser;
        
        @JsonProperty("success_rate")
        private double successRate;
    }
}
