package com.example.multiuser_online_editing.entity.collaboration;

import com.example.multiuser_online_editing.entity.document_management.Document;
import com.example.multiuser_online_editing.entity.user_management.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "collaboration_sessions")
public class CollaborationSession { // 实时协作实体类
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String sessionId; // WebSocket session ID

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    // 构造方法
    public CollaborationSession() {}

    public CollaborationSession(Document document, User user, String sessionId) {
        this.document = document;
        this.user = user;
        this.sessionId = sessionId;
        this.joinedAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

    @PreUpdate
    protected void onUpdate() {
        lastActivity = LocalDateTime.now();
    }
}