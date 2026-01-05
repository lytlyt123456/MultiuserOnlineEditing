package com.example.multiuser_online_editing.repository.communication;

import com.example.multiuser_online_editing.entity.communication.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 根据用户ID查找通知（按创建时间倒序）
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 查找用户的未读通知
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    // 统计用户的未读通知数量
    Long countByUserIdAndReadFalse(Long userId);

    // 标记用户的所有通知为已读
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    // 标记指定通知为已读
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :notificationId AND n.user.id = :userId")
    void markAsRead(@Param("notificationId") Long notificationId, @Param("userId") Long userId);
}