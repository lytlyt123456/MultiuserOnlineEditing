package com.example.multiuser_online_editing.entity.communication;

import com.example.multiuser_online_editing.entity.user_management.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conference_participants")
public class ConferenceParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conference_id")
    private VideoConference conference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ParticipantRole role = ParticipantRole.PARTICIPANT;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ParticipantStatus status = ParticipantStatus.JOINED;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "is_sharing_screen")
    private Boolean isSharingScreen = false;

    @Column(name = "is_video_enabled")
    private Boolean isVideoEnabled = true;

    @Column(name = "is_audio_enabled")
    private Boolean isAudioEnabled = true;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public VideoConference getConference() { return conference; }
    public void setConference(VideoConference conference) { this.conference = conference; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public ParticipantRole getRole() { return role; }
    public void setRole(ParticipantRole role) { this.role = role; }
    public ParticipantStatus getStatus() { return status; }
    public void setStatus(ParticipantStatus status) { this.status = status; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public LocalDateTime getLeftAt() { return leftAt; }
    public void setLeftAt(LocalDateTime leftAt) { this.leftAt = leftAt; }
    public Boolean getIsSharingScreen() { return isSharingScreen; }
    public void setIsSharingScreen(Boolean isSharingScreen) { this.isSharingScreen = isSharingScreen; }
    public Boolean getIsVideoEnabled() { return isVideoEnabled; }
    public void setIsVideoEnabled(Boolean isVideoEnabled) { this.isVideoEnabled = isVideoEnabled; }
    public Boolean getIsAudioEnabled() { return isAudioEnabled; }
    public void setIsAudioEnabled(Boolean isAudioEnabled) { this.isAudioEnabled = isAudioEnabled; }
}