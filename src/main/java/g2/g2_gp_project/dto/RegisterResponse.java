package g2.g2_gp_project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterResponse {
    private boolean success;
    private String message;
    private Integer userId;
    private String username;
    private String role;
}

