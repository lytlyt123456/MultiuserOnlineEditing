package com.example.multiuser_online_editing.entity.user_management;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_logs")
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // 外码为user_id
    @JsonIgnoreProperties({"operationLogs", "hibernateLazyInitializer", "handler"})
    private User user;

    @Column(nullable = false)
    private String operation; // 操作类型

    @Column(nullable = false)
    private String resourceType; // 操作资源类型

    private Long resourceId; // 操作资源ID

    @Column(nullable = false)
    private LocalDateTime operationTime; // 操作时间

    private String ipAddress; // 操作时的IP地址u

    @Column(length = 1000)
    private String details; // 操作细节

    @PrePersist
    protected void onCreate() {
        operationTime = LocalDateTime.now();
    }

    // Constructors, Getters and Setters
    public OperationLog() {}

    public OperationLog(User user, String operation, String resourceType, Long resourceId) {
        this.user = user;
        this.operation = operation;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public LocalDateTime getOperationTime() { return operationTime; }
    public void setOperationTime(LocalDateTime operationTime) { this.operationTime = operationTime; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}