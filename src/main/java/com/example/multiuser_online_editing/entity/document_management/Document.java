package com.example.multiuser_online_editing.entity.document_management;

import com.example.multiuser_online_editing.entity.collaboration.CollaborationSession;
import com.example.multiuser_online_editing.entity.collaboration.Comment;
import com.example.multiuser_online_editing.entity.collaboration.Task;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DocumentStatus status = DocumentStatus.EXISTS;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DocumentType type = DocumentType.RICH_TEXT; // 默认为富文本

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User owner;

    @ManyToMany
    @JoinTable(
            name = "document_collaborators",
            joinColumns = @JoinColumn(name = "document_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnoreProperties({"documents", "hibernateLazyInitializer", "handler"})
    private List<User> collaborators = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "document_tags",
            joinColumns = @JoinColumn(name = "document_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @JsonIgnoreProperties({"documents", "hibernateLazyInitializer", "handler"})
    private List<Tag> tags = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    @JsonIgnoreProperties({"children", "documents", "hibernateLazyInitializer", "handler"})
    private Folder folder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private Long version = 1L;

    // 自动保存相关字段
    private String autoSaveContent; // 临时草稿
    private LocalDateTime autoSaveTime; // 自动保存临时草稿的时间

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY)
    private List<CollaborationSession> collaborationSessions = new ArrayList<>();

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY)
    private List<Task> tasks = new ArrayList<>();

    // 构造方法、getter、setter
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (type == DocumentType.MARKDOWN && content == null) {
            content = ""; // 初始化Markdown内容
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public List<User> getCollaborators() { return collaborators; }
    public void setCollaborators(List<User> collaborators) { this.collaborators = collaborators; }
    public List<Tag> getTags() { return tags; }
    public void setTags(List<Tag> tags) { this.tags = tags; }
    public Folder getFolder() { return folder; }
    public void setFolder(Folder folder) { this.folder = folder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getAutoSaveContent() { return autoSaveContent; }
    public void setAutoSaveContent(String autoSaveContent) { this.autoSaveContent = autoSaveContent; }
    public LocalDateTime getAutoSaveTime() { return autoSaveTime; }
    public void setAutoSaveTime(LocalDateTime autoSaveTime) { this.autoSaveTime = autoSaveTime; }
    public DocumentType getType() { return type; }
    public void setType(DocumentType type) { this.type = type; }
}