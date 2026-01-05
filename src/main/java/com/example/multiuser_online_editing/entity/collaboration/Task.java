package com.example.multiuser_online_editing.entity.collaboration;

import com.example.multiuser_online_editing.entity.document_management.Document;
import com.example.multiuser_online_editing.entity.user_management.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    private String title; // 任务标题

    @Column(columnDefinition = "TEXT")
    private String description; // 任务描述

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TaskStatus status = TaskStatus.PENDING; // 任务状态

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM; // 任务优先级

    @Column(name = "due_date")
    private LocalDateTime dueDate; // 截止日期

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // 完成时间

    // 构造方法
    public Task() {}

    public Task(Document document, String title) {
        this.document = document;
        this.title = title;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // 如果状态变为完成，设置完成时间
        if (status == TaskStatus.COMPLETED && completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }

    // 检查用户是否有权限创建任务（只有文档所有者可以创建任务）
    public boolean canCreateTask(User user) {
        return document.getOwner().getId().equals(user.getId());
    }

    // 检查用户是否有权限操作任务（文档所有者或协作者）
    public boolean canOperateTask(User user) {
        boolean isOwner = document.getOwner().getId().equals(user.getId());
        boolean isCollaborator = document.getCollaborators().stream()
                .anyMatch(collaborator -> collaborator.getId().equals(user.getId()));
        return isOwner || isCollaborator;
    }

    // 获取所有相关的用户（文档所有者 + 所有协作者）
    public List<User> getRelatedUsers() {
        List<User> users = new ArrayList<>();
        users.add(document.getOwner());
        users.addAll(document.getCollaborators());
        return users;
    }
}

