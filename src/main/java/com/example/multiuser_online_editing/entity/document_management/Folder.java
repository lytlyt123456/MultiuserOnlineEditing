package com.example.multiuser_online_editing.entity.document_management;

import com.example.multiuser_online_editing.entity.user_management.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "folders")
public class Folder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Folder> children = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL)
    private List<Document> documents = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Folder getParent() { return parent; }
    public void setParent(Folder parent) { this.parent = parent; }
    public List<Folder> getChildren() { return children; }
    public void setChildren(List<Folder> children) { this.children = children; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public List<Document> getDocuments() { return documents; }
    public void setDocuments(List<Document> documents) { this.documents = documents; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        String folderName = this.name;
        Folder currentNode = this;
        while (currentNode.parent != null) {
            currentNode = currentNode.parent;
            folderName = currentNode.getName() + "/" + folderName;
        }

        return "/" + folderName;
    }
}