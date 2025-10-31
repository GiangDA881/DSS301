package g2.g2_gp_project.controller;

import g2.g2_gp_project.dto.ApiResponse;
import g2.g2_gp_project.dto.LoginRequest;
import g2.g2_gp_project.dto.LoginResponse;
import g2.g2_gp_project.service.AuthtService;
import g2.g2_gp_project.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthtService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.login(loginRequest.getUsername(), loginRequest.getPassword());
            if (response.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success("Login successful", response));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(response.getMessage(), response));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Object>> checkAuth() {
        return ResponseEntity.ok(ApiResponse.success("Authenticated", null));
    }

    @GetMapping("/check-role")
    public ResponseEntity<ApiResponse<Map<String, String>>> checkRole(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);
            Integer userId = jwtUtil.extractUserId(token);

            Map<String, String> roleInfo = new HashMap<>();
            roleInfo.put("username", username);
            roleInfo.put("userId", userId.toString());
            roleInfo.put("role", role);

            return ResponseEntity.ok(ApiResponse.success("Role retrieved successfully", roleInfo));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid token: " + e.getMessage()));
        }
    }
}
