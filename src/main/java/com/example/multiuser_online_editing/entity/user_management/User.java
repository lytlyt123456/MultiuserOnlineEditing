package com.example.multiuser_online_editing.entity.user_management;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(unique = true)
    private String username;

    @NotBlank
    @Size(max = 100)
    @Email
    @Column(unique = true)
    private String email;

    @Size(max = 20)
    @Column(unique = true)
    private String phone;

    @NotBlank
    @Size(max = 120)
    private String password;

    @Size(max = 100)
    private String fullName;

    @Size(max = 500)
    private String bio; // 个人简介（biography）

    @Size(max = 200)
    private String avatarPath;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role role = Role.VIEWER;

    private boolean enabled = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"user", "hibernateLazyInitializer", "handler"})
    private List<OperationLog> operationLogs = new ArrayList<>();
    // 通过OperationLog中的user字段来关联
    // 级联操作

//    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
//    private List<Document> ownDocuments = new ArrayList<>();
//
//    @ManyToMany(mappedBy = "collaborators")
//    private List<Document> collaborateDocuments = new ArrayList<>();
//
//    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
//    private List<Folder> ownFolders = new ArrayList<>();
//
//    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
//    private List<Tag> ownTags = new ArrayList<>();
//
//    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
//    private List<Template> ownTemplates = new ArrayList<>();
//
//    // 新增：用户创建的评论
//    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
//    private List<Comment> comments = new ArrayList<>();
//
//    // 新增：用户被提及的评论
//    @ManyToMany(mappedBy = "mentionedUsers")
//    private List<Comment> mentionedInComments = new ArrayList<>();
//
//    // 新增：用户的通知
//    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
//    private List<Notification> notifications = new ArrayList<>();
//
//    // 新增：用户的协作会话
//    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
//    private List<CollaborationSession> collaborationSessions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors, Getters and Setters
    public User() {}

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<OperationLog> getOperationLogs() { return operationLogs; }
    public void setOperationLogs(List<OperationLog> operationLogs) { this.operationLogs = operationLogs; }
}