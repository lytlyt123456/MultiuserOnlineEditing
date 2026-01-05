package com.example.multiuser_online_editing.controller.user_management;

import com.example.multiuser_online_editing.controller.ApiResponse;
import com.example.multiuser_online_editing.entity.user_management.OperationLog;
import com.example.multiuser_online_editing.entity.user_management.Role;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.example.multiuser_online_editing.service.user_management.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Object>> updateProfile(@RequestBody UpdateProfileRequest updateRequest) {
        try {
            // 从安全上下文获取当前用户ID
            Long currentUserId = getCurrentUserId();
            User user = userService.updateProfile(currentUserId,
                    updateRequest.getFullName(),
                    updateRequest.getBio(),
                    updateRequest.getPhone(),
                    updateRequest.getEmail());

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());

            return ResponseEntity.ok(ApiResponse.success("更新个人信息成功！", userInfo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/get-profile")
    public ResponseEntity<ApiResponse<Object>> getProfile() {
        try {
            // 从安全上下文获取当前用户ID
            Long currentUserId = getCurrentUserId();
            User user = userService.getUserProfile(currentUserId);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("phone", user.getPhone());
            userInfo.put("full_name", user.getFullName());
            userInfo.put("role", user.getRole());
            userInfo.put("biography", user.getBio());
            userInfo.put("avatar_path", user.getAvatarPath());

            return ResponseEntity.ok(ApiResponse.success("更新个人信息成功！", userInfo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{username}/get-profile-by-username")
    public ResponseEntity<ApiResponse<Object>> getProfile(@PathVariable String username) {
        try {
            // 从安全上下文获取当前用户ID
            User user = userService.getUserProfile(username);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("phone", user.getPhone());
            userInfo.put("full_name", user.getFullName());
            userInfo.put("role", user.getRole());
            userInfo.put("biography", user.getBio());
            userInfo.put("avatar_path", user.getAvatarPath());

            return ResponseEntity.ok(ApiResponse.success("更新个人信息成功！", userInfo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{username}/get-id-by-username")
    public ResponseEntity<ApiResponse<Object>> getUserId(@PathVariable String username) {
        try {
            Long id = userService.getUserId(username);
            Map<String, Object> response = new HashMap<>();
            response.put("userId", id);
            return ResponseEntity.ok(ApiResponse.success("更新个人信息成功！", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userId}/get-profile-by-id")
    public ResponseEntity<ApiResponse<Object>> getProfile(@PathVariable Long userId) {
        try {
            // 从安全上下文获取当前用户ID
            User user = userService.getUserProfile(userId);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("phone", user.getPhone());
            userInfo.put("full_name", user.getFullName());
            userInfo.put("role", user.getRole());
            userInfo.put("biography", user.getBio());
            userInfo.put("avatar_path", user.getAvatarPath());

            return ResponseEntity.ok(ApiResponse.success("更新个人信息成功！", userInfo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<Object>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            Long currentUserId = getCurrentUserId();
            String fileName = userService.uploadAvatar(currentUserId, file);
            return ResponseEntity.ok(ApiResponse.success("头像上传成功！"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/remove-avatar")
    public ResponseEntity<ApiResponse<Object>> removeAvatar() {
        try {
            Long currentUserId = getCurrentUserId();
            userService.removeAvatar(currentUserId);
            return ResponseEntity.ok(ApiResponse.success("头像删除成功！"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<Object>> changeUserRole(@PathVariable Long userId, @RequestBody Map<String, String> request) {
        try {
            Role newRole = Role.valueOf(request.get("role"));
            User user = userService.changeRole(userId, newRole);
            return ResponseEntity.ok(ApiResponse.success("角色更改成功！"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 申请升级角色
    @PostMapping("/request-upgrade")
    public ResponseEntity<ApiResponse<Object>> requestRoleUpgrade() {
        try {
            Long currentUserId = getCurrentUserId();
            userService.requestRoleUpgrade(currentUserId);
            return ResponseEntity.ok(ApiResponse.success("申请升级角色成功，等待管理员审核"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 获取所有升级申请（管理员专用）
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/upgrade-requests")
    public ResponseEntity<ApiResponse<Object>> getUpgradeRequests() {
        try {
            List<Long> requestUserIds = userService.getUpgradeRequests();
            return ResponseEntity.ok(ApiResponse.success("获取升级申请成功", requestUserIds));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Object>> getUserLogs() {
        try {
            Long currentUserId = getCurrentUserId();
            List<OperationLog> logs = userService.getUserOperationLogs(currentUserId);

            List<Map<String, Object>> logDTOs = logs.stream().map(log -> {
                Map<String, Object> logMap = new HashMap<>();
                logMap.put("id", log.getId());
                logMap.put("operation", log.getOperation());
                logMap.put("resourceType", log.getResourceType());
                logMap.put("resourceId", log.getResourceId());
                logMap.put("operationTime", log.getOperationTime());
                logMap.put("ipAddress", log.getIpAddress());
                logMap.put("details", log.getDetails());
                return logMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("获取操作日志成功", logDTOs));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userId}/logs")
    public ResponseEntity<ApiResponse<Object>> getUserLogs(@PathVariable Long userId) {
        try {
            List<OperationLog> logs = userService.getUserOperationLogs(userId);

            List<Map<String, Object>> logDTOs = logs.stream().map(log -> {
                Map<String, Object> logMap = new HashMap<>();
                logMap.put("id", log.getId());
                logMap.put("username", log.getUser().getUsername());
                logMap.put("userId", log.getUser().getId());
                logMap.put("operation", log.getOperation());
                logMap.put("resourceType", log.getResourceType());
                logMap.put("resourceId", log.getResourceId());
                logMap.put("operationTime", log.getOperationTime());
                logMap.put("ipAddress", log.getIpAddress());
                logMap.put("details", log.getDetails());
                return logMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("获取操作日志成功", logDTOs));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private Long getCurrentUserId() {
        return userService.getCurrentUserId();
    }
}

class UpdateProfileRequest {
    private String fullName;
    private String bio;
    private String phone;
    private String email;

    // Getters and Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
}