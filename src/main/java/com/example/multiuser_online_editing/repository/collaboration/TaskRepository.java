package com.example.multiuser_online_editing.repository.collaboration;

import com.example.multiuser_online_editing.entity.collaboration.Task;
import com.example.multiuser_online_editing.entity.collaboration.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // 根据文档ID查找所有任务（按创建时间倒序）
    List<Task> findByDocumentIdOrderByCreatedAtDesc(Long documentId);

    // 根据文档ID和状态查找任务
    List<Task> findByDocumentIdAndStatusOrderByDueDateAsc(Long documentId, TaskStatus status);

    // 查找用户相关的任务（用户是文档所有者或协作者）
    @Query("SELECT t FROM Task t WHERE " +
            "t.document.owner.id = :userId OR " +
            ":userId IN (SELECT c.id FROM t.document.collaborators c) " +
            "ORDER BY t.createdAt DESC")
    List<Task> findUserRelatedTasks(@Param("userId") Long userId);

    // 查找待处理的任务
    @Query("SELECT t FROM Task t WHERE " +
            "t.status = 'PENDING' AND " +
            "(t.document.owner.id = :userId OR " +
            ":userId IN (SELECT c.id FROM t.document.collaborators c)) " +
            "ORDER BY t.dueDate ASC")
    List<Task> findPendingTasks(@Param("userId") Long userId);

    // 查找进行中的任务
    @Query("SELECT t FROM Task t WHERE " +
            "t.status = 'IN_PROGRESS' AND " +
            "(t.document.owner.id = :userId OR " +
            ":userId IN (SELECT c.id FROM t.document.collaborators c)) " +
            "ORDER BY t.dueDate ASC")
    List<Task> findInProgressTasks(@Param("userId") Long userId);

    // 查找已过期的任务
    @Query("SELECT t FROM Task t WHERE " +
            "t.dueDate < :now AND " +
            "t.status IN ('PENDING', 'IN_PROGRESS') AND " +
            "(t.document.owner.id = :userId OR " +
            ":userId IN (SELECT c.id FROM t.document.collaborators c)) " +
            "ORDER BY t.dueDate ASC")
    List<Task> findOverdueTasks(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    // 统计文档的任务数量
    Long countByDocumentId(Long documentId);

    // 统计文档的待处理任务数量
    Long countByDocumentIdAndStatus(Long documentId, TaskStatus status);

    // 更新任务状态
    @Modifying
    @Query("UPDATE Task t SET t.status = :status, t.updatedAt = CURRENT_TIMESTAMP WHERE t.id = :taskId")
    void updateTaskStatus(@Param("taskId") Long taskId, @Param("status") TaskStatus status);

    // 删除文档的所有任务
    @Modifying
    @Query("DELETE FROM Task t WHERE t.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);

    // 查找即将到期的任务（3天内到期）
    @Query("SELECT t FROM Task t WHERE " +
            "t.dueDate BETWEEN :start AND :end AND " +
            "t.status IN ('PENDING', 'IN_PROGRESS')")
    List<Task> findUpcomingTasks(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 查找用户创建的任务
    @Query("SELECT t FROM Task t WHERE t.document.owner.id = :userId ORDER BY t.createdAt DESC")
    List<Task> findTasksCreatedByUser(@Param("userId") Long userId);
}