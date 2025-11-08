package g2.g2_gp_project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetailSummary {
    private String category;
    private String month;
    private String country;
    private Double totalRevenue;
}

