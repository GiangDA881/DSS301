package g2.g2_gp_project.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductAnalysisRequest {
    private String fromDate; // yyyy-MM-dd
    private String toDate; // yyyy-MM-dd
    private String[] countries;
    private Integer topN;
    private Boolean removeReturns = true; // Remove negative quantities by default
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class ProductAnalysisResponse {
    private Integer rank;
    private String stockCode;
    private String description;
    private String country;
    private Integer totalQuantity;
    private BigDecimal totalRevenue;
    private BigDecimal marketSharePercent;
}