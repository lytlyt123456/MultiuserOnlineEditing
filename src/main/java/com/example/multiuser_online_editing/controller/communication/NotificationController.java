package com.example.multiuser_online_editing.controller.communication;

import com.example.multiuser_online_editing.controller.ApiResponse;
import com.example.multiuser_online_editing.entity.communication.Notification;
import com.example.multiuser_online_editing.service.communication.NotificationService;
import com.example.multiuser_online_editing.service.user_management.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    /**
     * 获取用户通知
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getUserNotifications() {
        try {
            Long currentUserId = userService.getCurrentUserId();

            List<Notification> notifications = notificationService.getUserNotifications(currentUserId);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("notifications", notificationsToMaps(notifications));
            responseData.put("unreadCount", notificationService.getUnreadCount(currentUserId));

            return ResponseEntity.ok(ApiResponse.success("获取通知成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Object>> markAsRead(@PathVariable Long notificationId) {
        try {
            Long currentUserId = userService.getCurrentUserId();

            notificationService.markAsRead(notificationId, currentUserId);

            return ResponseEntity.ok(ApiResponse.success("通知已标记为已读"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 标记所有通知为已读
     */
    @PutMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<Object>> markAllAsRead() {
        try {
            Long currentUserId = userService.getCurrentUserId();

            notificationService.markAllAsRead(currentUserId);

            return ResponseEntity.ok(ApiResponse.success("所有通知已标记为已读"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Object>> getUnreadCount() {
        try {
            Long currentUserId = userService.getCurrentUserId();

            Long unreadCount = notificationService.getUnreadCount(currentUserId);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("unreadCount", unreadCount);

            return ResponseEntity.ok(ApiResponse.success("获取未读数量成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Object>> deleteNotification(@PathVariable Long notificationId) {
        try {
            Long currentUserId = userService.getCurrentUserId();

            notificationService.deleteNotification(notificationId, currentUserId);

            return ResponseEntity.ok(ApiResponse.success("通知删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 工具方法：通知列表转换为Map
    private List<Map<String, Object>> notificationsToMaps(List<Notification> notifications) {
        return notifications.stream()
                .map(this::notificationToMap)
                .toList();
    }

    private Map<String, Object> notificationToMap(Notification notification) {
        Map<String, Object> notificationMap = new HashMap<>();
        notificationMap.put("id", notification.getId());
        notificationMap.put("title", notification.getTitle());
        notificationMap.put("message", notification.getMessage());
        notificationMap.put("read", notification.getRead());
        notificationMap.put("createdAt", notification.getCreatedAt());
        return notificationMap;
    }
}