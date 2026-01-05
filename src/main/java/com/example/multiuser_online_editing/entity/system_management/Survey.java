package com.example.multiuser_online_editing.entity.system_management;

import com.example.multiuser_online_editing.entity.user_management.User;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "surveys")
public class Survey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"operationLogs"})
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"survey"})
    private List<SurveyAnswer> answers = new ArrayList<>();

    // 计算Likert问题的平均分（只计算五点量表问题的平均分）
    @Transient
    private Double averageLikertScore;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 计算平均分的方法
    public Double calculateAverageLikertScore() {
        if (answers == null || answers.isEmpty()) {
            return 0.0;
        }

        double sum = 0;
        int count = 0;

        for (SurveyAnswer answer : answers) {
            if (answer.getLikertAnswer() != null) {
                sum += answer.getLikertAnswer();
                count++;
            }
        }

        return count > 0 ? sum / count : 0.0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<SurveyAnswer> getAnswers() { return answers; }
    public void setAnswers(List<SurveyAnswer> answers) { this.answers = answers; }
    public Double getAverageLikertScore() {
        if (averageLikertScore == null) {
            averageLikertScore = calculateAverageLikertScore();
        }
        return averageLikertScore;
    }
}