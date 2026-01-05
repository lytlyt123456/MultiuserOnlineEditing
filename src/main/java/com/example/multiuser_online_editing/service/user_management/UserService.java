package com.example.multiuser_online_editing.service.user_management;

import com.example.multiuser_online_editing.entity.user_management.OperationLog;
import com.example.multiuser_online_editing.entity.user_management.Role;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.example.multiuser_online_editing.repository.user_management.OperationLogRepository;
import com.example.multiuser_online_editing.repository.user_management.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.upload.path}")
    private String uploadPath;

    // 存储申请升级角色的用户ID列表
    private List<Long> upgradeRequests = new ArrayList<>();

    public User registerUser(String username, String email, String phone, String password) {
        if (username == null || username.isEmpty()) {
            throw new RuntimeException("Username is null value!");
        }
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email is null value!");
        }
        if (phone == null || phone.isEmpty()) {
            throw new RuntimeException("Phone is null value!");
        }
        if (password == null || password.isEmpty()) {
            throw new RuntimeException("Password is null value!");
        }

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username is already taken!");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already in use!");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new RuntimeException("Phone number is already in use!");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.VIEWER); // 默认角色

        User savedUser = userRepository.save(user);

        // 记录操作日志
        logOperation(savedUser.getId(), "USER_REGISTER", "USER", savedUser.getId(), "User registered successfully");

        return savedUser;
    }

    public Long getUserId(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getId();
    }

    public User updateProfile(Long userId, String fullName, String bio, String phone, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (fullName != null) user.setFullName(fullName);
        if (bio != null) user.setBio(bio);
        if (phone != null) {
            if (!phone.equals(user.getPhone()) && userRepository.existsByPhone(phone)) {
                throw new RuntimeException("Phone number is already in use!");
            }
            user.setPhone(phone);
        }
        if (email != null) {
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new RuntimeException("Email is already in use!");
            }
            user.setEmail(email);
        }

        User updatedUser = userRepository.save(user);

        // 记录操作日志
        logOperation(userId, "UPDATE_PROFILE", "USER", userId, "User profile updated");

        return updatedUser;
    }

    public User getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user;
    }

    public User getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user;
    }

    public String uploadAvatar(Long userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        // 创建上传目录
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // 生成唯一文件名
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")); // 提取文件后缀名
        String newFileName = UUID.randomUUID().toString() + fileExtension;

        // 保存文件
        Path filePath = uploadDir.resolve(newFileName);
        Files.copy(file.getInputStream(), filePath);

        // 删除旧头像
        if (user.getAvatarPath() != null) { // 存在旧头像
            Path oldAvatarPath = Paths.get(user.getAvatarPath());
            if (Files.exists(oldAvatarPath)) {
                Files.delete(oldAvatarPath);
            }
        }

        user.setAvatarPath(filePath.toString());
        userRepository.save(user);

        // 记录操作日志
        logOperation(userId, "UPLOAD_AVATAR", "USER", userId, "Avatar uploaded");

        return newFileName;
    }

    public void removeAvatar(Long userId) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 删除旧头像
        if (user.getAvatarPath() != null) { // 存在旧头像
            Path oldAvatarPath = Paths.get(user.getAvatarPath());
            if (Files.exists(oldAvatarPath)) {
                Files.delete(oldAvatarPath);
            }

            user.setAvatarPath(null);
            userRepository.save(user);
        }
    }

    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 记录操作日志
        logOperation(user.getId(), "RESET_PASSWORD", "USER", user.getId(), "Password reset");
    }

    public User changeRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role pre_role = user.getRole();

        user.setRole(newRole);

        if (user.getRole() == Role.EDITOR)
            upgradeRequests.remove(user.getId());

        User updatedUser = userRepository.save(user);

        // 记录操作日志
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName(); // 获取当前用户的用户名
        logOperation(getCurrentUserId(), "CHANGE_ROLE", "USER", userId,
                "Role changed from " + pre_role + " to " + newRole + " by " + currentUsername);
        logOperation(userId, "CHANGE_ROLE", "USER", userId,
                "Role changed from " + pre_role + " to " + newRole + " by ADMIN");

        return updatedUser;
    }

    // 申请升级角色为编辑者
    public void requestRoleUpgrade(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 只有VIEWER可以申请升级
        if (user.getRole() != Role.VIEWER) {
            throw new RuntimeException("只有查看者可以申请升级角色");
        }

        // 避免重复申请（重复申请不记录日志）
        if (!upgradeRequests.contains(userId)) {
            upgradeRequests.add(userId);
        }

        // 记录操作日志（重复申请也要记录日志）
        logOperation(userId, "REQUEST_ROLE_UPGRADE", "USER", userId, "Apply to upgrade the role to Editor");
    }

    // 获取所有申请升级角色的用户ID列表
    public List<Long> getUpgradeRequests() {
        return new ArrayList<>(upgradeRequests);
    }

    // 移除申请（批准或拒绝后）
    public void removeUpgradeRequest(Long userId) {
        upgradeRequests.remove(userId);
    }

    public List<OperationLog> getUserOperationLogs(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return operationLogRepository.findByUserIdOrderByOperationTimeDesc(userId);
    }

    public void logOperation(Long userId, String operation, String resourceType, Long resourceId, String details) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            OperationLog log = new OperationLog(user, operation, resourceType, resourceId);
            log.setDetails(details);
            operationLogRepository.save(log);
        }
    }

    public Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        return user != null ? user.getId() : null;
    }
}