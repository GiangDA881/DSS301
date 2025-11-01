// java
package g2.g2_gp_project.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {
    private String orderId;
    private String customerName;
    private LocalDate orderDate;
    private BigDecimal totalAmount;
    private String status;
}