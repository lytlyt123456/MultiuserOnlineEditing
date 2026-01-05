package com.example.multiuser_online_editing.repository.user_management;

import com.example.multiuser_online_editing.entity.user_management.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    List<OperationLog> findByUserIdOrderByOperationTimeDesc(Long userId);
    List<OperationLog> findByResourceTypeAndResourceIdOrderByOperationTimeDesc(String resourceType, Long resourceId);
    Long countByUserId(Long userId);
}