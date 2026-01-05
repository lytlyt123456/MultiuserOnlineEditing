package com.example.multiuser_online_editing.service.collaboration;

import com.example.multiuser_online_editing.entity.collaboration.Task;
import com.example.multiuser_online_editing.entity.collaboration.TaskPriority;
import com.example.multiuser_online_editing.entity.collaboration.TaskStatus;
import com.example.multiuser_online_editing.entity.document_management.Document;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.example.multiuser_online_editing.repository.document_management.DocumentRepository;
import com.example.multiuser_online_editing.repository.collaboration.TaskRepository;
import com.example.multiuser_online_editing.repository.user_management.UserRepository;
import com.example.multiuser_online_editing.service.communication.NotificationService;
import com.example.multiuser_online_editing.service.user_management.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 创建任务（只有文档所有者可以创建）
     */
    public Task createTask(Long documentId, String title, String description,
                           TaskPriority priority, LocalDateTime dueDate) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        // 检查权限：只有文档所有者可以创建任务
        Long currentUserId = getCurrentUserId();
        if (!document.getOwner().getId().equals(currentUserId)) {
            throw new RuntimeException("只有文档所有者可以创建任务");
        }

        Task task = new Task(document, title);
        task.setDescription(description);
        task.setPriority(priority);
        task.setDueDate(dueDate);
        task.setStatus(TaskStatus.PENDING);

        Task savedTask = taskRepository.save(task);

        // 通知所有相关用户
        notifyTaskCreated(savedTask);

        // 广播新任务通知
        messagingTemplate.convertAndSend(
                "/topic/document/" + documentId + "/tasks",
                getDocumentTasks(documentId)
        );

        for (User user : task.getRelatedUsers()) {
            messagingTemplate.convertAndSend(
                    "/topic/user/" + user.getId() + "/queue/task-updates",
                    task
            );
        }

        return savedTask;
    }

    /**
     * 更新任务状态
     */
    public Task updateTaskStatus(Long taskId, TaskStatus status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        // 检查权限：文档所有者或协作者可以更新状态
        if (!task.canOperateTask(getCurrentUser())) {
            throw new RuntimeException("无权更新此任务状态");
        }

        TaskStatus oldStatus = task.getStatus();
        task.setStatus(status);
        Task updatedTask = taskRepository.save(task);

        // 如果任务状态变为完成，发送完成通知
        if (status == TaskStatus.COMPLETED && oldStatus != TaskStatus.COMPLETED) {
            notifyTaskCompleted(updatedTask);
        }

        // 广播任务更新
        messagingTemplate.convertAndSend(
                "/topic/document/" + task.getDocument().getId() + "/tasks",
                getDocumentTasks(task.getDocument().getId())
        );

        for (User user : task.getRelatedUsers()) {
            messagingTemplate.convertAndSend(
                    "/topic/user/" + user.getId() + "/queue/task-updates",
                    task
            );
        }

        return updatedTask;
    }

    /**
     * 获取文档的所有任务
     */
    public List<Task> getDocumentTasks(Long documentId) {
        return taskRepository.findByDocumentIdOrderByCreatedAtDesc(documentId);
    }

    /**
     * 获取用户相关的任务
     */
    public List<Task> getUserTasks(Long userId) {
        return taskRepository.findUserRelatedTasks(userId);
    }

    /**
     * 获取待处理的任务列表
     */
    public List<Task> getPendingTasks(Long userId) {
        return taskRepository.findPendingTasks(userId);
    }

    /**
     * 通知任务创建
     */
    private void notifyTaskCreated(Task task) {
        String message = "新任务: " + task.getTitle();
        if (task.getDueDate() != null) {
            message += " (截止: " + task.getDueDate().toLocalDate() + ")";
        }

        for (User user : task.getRelatedUsers()) {
            if (!user.getId().equals(getCurrentUserId())) { // 不通知创建者自己
                notificationService.sendNotification(user.getId(), "新任务分配", message);
            }
        }
    }

    /**
     * 通知任务完成
     */
    private void notifyTaskCompleted(Task task) {
        String message = "任务已完成: " + task.getTitle();
        User currentUser = getCurrentUser();

        for (User user : task.getRelatedUsers()) {
            if (!user.getId().equals(currentUser.getId())) { // 不通知完成者自己
                notificationService.sendNotification(
                        user.getId(),
                        "任务完成",
                        currentUser.getUsername() + " 完成了任务: " + task.getTitle()
                );
            }
        }
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        return userService.getCurrentUserId();
    }

    /**
     * 获取当前用户
     */
    private User getCurrentUser() {
        Long userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}