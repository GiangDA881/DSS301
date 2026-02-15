package g2.g2_gp_project.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    private String customerName;
    private String orderDate; // yyyy-MM-dd
    private String status;
    private List<CreateOrderItemRequest> items;
}
