package com.example.multiuser_online_editing.service.communication;

import com.example.multiuser_online_editing.entity.communication.Notification;
import com.example.multiuser_online_editing.repository.communication.NotificationRepository;
import com.example.multiuser_online_editing.repository.user_management.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 发送通知
     */
    public void sendNotification(Long userId, String title, String message) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Notification notification = new Notification();
        notification.setUser(userRepository.getReferenceById(userId));
        notification.setTitle(title);
        notification.setMessage(message);

        Notification savedNotification = notificationRepository.save(notification);

        // 实时推送通知
        messagingTemplate.convertAndSend(
                "/topic/user/" + userId + "/queue/notifications",
                savedNotification
        );
    }

    /**
     * 标记通知为已读
     */
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.markAsRead(notificationId, userId);
    }

    /**
     * 获取用户的通知
     */
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取用户的未读通知数量
     */
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * 标记用户所有通知为已读
     */
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("通知不存在"));

        // 检查权限：只能删除自己的通知
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权删除此通知");
        }

        notificationRepository.delete(notification);
    }
}