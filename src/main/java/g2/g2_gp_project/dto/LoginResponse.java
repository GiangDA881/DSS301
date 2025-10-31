package g2.g2_gp_project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private boolean success;
    private String message;
    private String username;
    private Integer userId;
    private String role;
    private String roleDisplayName;
    private String token;
    private String tokenType = "Bearer";

    // Constructor without token for backward compatibility
    public LoginResponse(boolean success, String message, String username, Integer userId) {
        this.success = success;
        this.message = message;
        this.username = username;
        this.userId = userId;
    }
}
