package g2.g2_gp_project.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderItemRequest {
    private String productId; // optional stock code
    private String description;
    private Integer quantity;
    private BigDecimal unitPrice;
}
