package com.example.multiuser_online_editing.entity.communication;

import com.example.multiuser_online_editing.entity.user_management.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conference_messages")
public class ConferenceMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conference_id")
    private VideoConference conference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public VideoConference getConference() { return conference; }
    public void setConference(VideoConference conference) { this.conference = conference; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}