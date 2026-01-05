package com.example.multiuser_online_editing.controller.user_management;

import com.example.multiuser_online_editing.controller.ApiResponse;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.example.multiuser_online_editing.service.user_management.UserService;
import com.example.multiuser_online_editing.util.JwtUtils;
import com.example.multiuser_online_editing.service.user_management.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserService userService;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/signin") // 登录请求
    public ResponseEntity<ApiResponse<Object>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            String jwt = jwtUtils.generateJwtToken(userDetails);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("token", jwt);
            userInfo.put("id", userDetails.getId());
            userInfo.put("username", userDetails.getUsername());
            userInfo.put("email", userDetails.getEmail());
            userInfo.put("role", userDetails.getAuthorities().iterator().next().getAuthority());

            Long userId = userService.getUserId(loginRequest.getUsername());
            userService.logOperation(userId, "USER_LOGIN", "USER", userId, "User login successfully");

            return ResponseEntity.ok(ApiResponse.success("登录成功", userInfo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户名或密码错误"));
        }
    }

    @PostMapping("/signup") // 用户注册
    public ResponseEntity<ApiResponse<Object>> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        try {
            // 调用userService处理业务
            User user = userService.registerUser(
                    signUpRequest.getUsername(),
                    signUpRequest.getEmail(),
                    signUpRequest.getPhone(),
                    signUpRequest.getPassword());

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());

            return ResponseEntity.ok(ApiResponse.success("用户注册成功!", userInfo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest resetRequest) {
        try {
            userService.resetPassword(resetRequest.getEmail(), resetRequest.getNewPassword());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Password reset successfully");

            return ResponseEntity.ok(ApiResponse.success("重置密码成功！", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

// 请求DTO类
class LoginRequest {
    private String username;
    private String password;

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

class SignupRequest {
    private String username;
    private String email;
    private String phone;
    private String password;

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

class ResetPasswordRequest {
    private String email;
    private String newPassword;

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}