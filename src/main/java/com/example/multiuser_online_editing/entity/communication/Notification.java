package com.example.multiuser_online_editing.entity.communication;

import com.example.multiuser_online_editing.entity.user_management.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 接收通知的用户

    private String title; // 通知标题

    @Column(columnDefinition = "TEXT")
    private String message; // 通知内容

    @Column(name = "`read`") // read是MySQL保留字，不能直接作为列名，需添加转义
    private Boolean read = false; // 是否已读

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 构造方法
    public Notification() {
        createdAt = LocalDateTime.now();
    }

    public Notification(User user, String title, String message) {
        this.user = user;
        this.title = title;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}