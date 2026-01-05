package com.example.multiuser_online_editing.entity.communication;

import com.example.multiuser_online_editing.entity.document_management.Document;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "video_conferences")
public class VideoConference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(nullable = false)
    private String conferenceId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ConferenceStatus status = ConferenceStatus.ACTIVE;

    @Column(name = "max_participants")
    private Integer maxParticipants = 10;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @OneToMany(mappedBy = "conference", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"conference", "user"})
    private List<ConferenceParticipant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "conference", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"conference", "user"})
    private List<ConferenceMessage> messages = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        conferenceId = generateConferenceId();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateConferenceId() {
        return "conf_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }
    public String getConferenceId() { return conferenceId; }
    public void setConferenceId(String conferenceId) { this.conferenceId = conferenceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public ConferenceStatus getStatus() { return status; }
    public void setStatus(ConferenceStatus status) { this.status = status; }
    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    public List<ConferenceParticipant> getParticipants() { return participants; }
    public void setParticipants(List<ConferenceParticipant> participants) { this.participants = participants; }
    public List<ConferenceMessage> getMessages() { return messages; }
    public void setMessages(List<ConferenceMessage> messages) { this.messages = messages; }
}