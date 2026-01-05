package com.example.multiuser_online_editing.repository.collaboration;

import com.example.multiuser_online_editing.entity.collaboration.CollaborationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollaborationSessionRepository extends JpaRepository<CollaborationSession, Long> {

    // 根据文档ID查找所有协作会话
    List<CollaborationSession> findByDocumentId(Long documentId);

    // 根据用户ID和文档ID查找协作会话
    Optional<CollaborationSession> findByUserIdAndDocumentId(Long userId, Long documentId);

    // 根据Session ID查找协作会话
    Optional<CollaborationSession> findBySessionId(String sessionId);

    // 根据用户ID查找协作会话
    List<CollaborationSession> findByUserId(Long userId);

    // 删除指定文档的所有协作会话
    @Modifying
    @Query("DELETE FROM CollaborationSession cs WHERE cs.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);

    // 删除指定用户的协作会话
    @Modifying
    @Query("DELETE FROM CollaborationSession cs WHERE cs.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    // 删除过期的协作会话（最后活动时间超过指定时间）
    @Modifying
    @Query("DELETE FROM CollaborationSession cs WHERE cs.lastActivity < :expireTime")
    void deleteByLastActivityBefore(@Param("expireTime") LocalDateTime expireTime);

    // 获取文档的在线用户数量
    @Query("SELECT COUNT(cs) FROM CollaborationSession cs WHERE cs.document.id = :documentId")
    Long countByDocumentId(@Param("documentId") Long documentId);
}