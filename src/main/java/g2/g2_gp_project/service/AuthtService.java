package g2.g2_gp_project.service;

import g2.g2_gp_project.dto.LoginResponse;
import g2.g2_gp_project.dto.RegisterResponse;
import g2.g2_gp_project.entity.User;
import g2.g2_gp_project.repository.UserRepository;
import g2.g2_gp_project.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthtService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(String username, String password) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (passwordEncoder.matches(password, user.getPassword())) {
                // Generate JWT token with role
                String token = jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole());

                LoginResponse response = new LoginResponse(
                    true,
                    "Login successful",
                    user.getUsername(),
                    user.getId()
                );
                response.setToken(token);
                response.setTokenType("Bearer");
                response.setRole(user.getRole());
                response.setRoleDisplayName(user.getRole());

                return response;
            } else {
                return new LoginResponse(false, "Invalid username or password", null, null);
            }
        } catch (Exception e) {
            return new LoginResponse(false, "Login failed: " + e.getMessage(), null, null);
        }
    }

    // Helper method để tạo user mới với password đã mã hóa
    public User createUser(String username, String rawPassword) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole("Business Analyst"); // Set default role
        return userRepository.save(user);
    }

    // Method để tạo user với role tùy chỉnh
    public User createUserWithRole(String username, String rawPassword, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        return userRepository.save(user);
    }

    // Method để đăng ký user mới
    public RegisterResponse register(String username, String password, String role) {
        try {
            // Check if username already exists
            if (userRepository.findByUsername(username).isPresent()) {
                return new RegisterResponse(false, "Username already exists", null, null, null);
            }

            // Validate role (nếu có)
            String userRole = (role != null && !role.isEmpty()) ? role : "Business Analyst";

            // Create user
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(userRole);

            User savedUser = userRepository.save(user);

            return new RegisterResponse(
                true,
                "Registration successful",
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole()
            );
        } catch (Exception e) {
            return new RegisterResponse(false, "Registration failed: " + e.getMessage(), null, null, null);
        }
    }
}