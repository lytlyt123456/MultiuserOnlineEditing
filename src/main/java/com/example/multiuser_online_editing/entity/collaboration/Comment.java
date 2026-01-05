package com.example.multiuser_online_editing.entity.collaboration;

import com.example.multiuser_online_editing.entity.document_management.Document;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "TEXT")
    private String content; // 评论内容

    private Integer position; // 在文档中的位置

    @Column(name = "resolved")
    private Boolean resolved = false; // 是否已解决

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 父评论（用于回复）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnoreProperties({"replies", "hibernateLazyInitializer", "handler"})
    private Comment parent;

    // 子评论（回复）
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"parent", "hibernateLazyInitializer", "handler"})
    private List<Comment> replies = new ArrayList<>();

    // 被提及的用户
    @ManyToMany
    @JoinTable(
            name = "comment_mentions",
            joinColumns = @JoinColumn(name = "comment_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> mentionedUsers = new ArrayList<>();

    // 构造方法
    public Comment() {}

    public Comment(Document document, User user, String content, Integer position) {
        this.document = document;
        this.user = user;
        this.content = content;
        this.position = position;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    public Boolean getResolved() { return resolved; }
    public void setResolved(Boolean resolved) { this.resolved = resolved; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Comment getParent() { return parent; }
    public void setParent(Comment parent) { this.parent = parent; }
    public List<Comment> getReplies() { return replies; }
    public void setReplies(List<Comment> replies) { this.replies = replies; }
    public List<User> getMentionedUsers() { return mentionedUsers; }
    public void setMentionedUsers(List<User> mentionedUsers) { this.mentionedUsers = mentionedUsers; }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}