package g2.g2_gp_project.controller;

import g2.g2_gp_project.dto.ApiResponse;
import g2.g2_gp_project.dto.CreateUserRequest;
import g2.g2_gp_project.entity.User;
import g2.g2_gp_project.service.AuthtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final AuthtService authService;

    // Endpoint để tạo user test - Dùng JSON body
    @PostMapping("/create-user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTestUser(@RequestBody CreateUserRequest request) {
        try {
            User user = authService.createUser(request.getUsername(), request.getPassword());

            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", user.getId());
            userData.put("username", user.getUsername());
            userData.put("role", user.getRole());
            userData.put("createdAt", user.getCreatedAt());

            return ResponseEntity.ok(ApiResponse.success("User created successfully", userData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Error creating user: " + e.getMessage()));
        }
    }

    // Endpoint để tạo user với role tùy chỉnh - Dùng JSON body
    @PostMapping("/create-user-with-role")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createUserWithRole(@RequestBody CreateUserRequest request) {
        try {
            User user = authService.createUserWithRole(request.getUsername(), request.getPassword(), request.getRole());

            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", user.getId());
            userData.put("username", user.getUsername());
            userData.put("role", user.getRole());
            userData.put("createdAt", user.getCreatedAt());

            return ResponseEntity.ok(ApiResponse.success("User with role created successfully", userData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Error creating user: " + e.getMessage()));
        }
    }
}
