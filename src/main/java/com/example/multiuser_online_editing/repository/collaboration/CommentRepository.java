package com.example.multiuser_online_editing.repository.collaboration;

import com.example.multiuser_online_editing.entity.collaboration.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 根据文档ID查找所有评论（按创建时间升序）
    List<Comment> findByDocumentIdOrderByCreatedAtAsc(Long documentId);

    // 根据文档ID查找顶级评论（没有父评论的评论）
    List<Comment> findByDocumentIdAndParentIsNullOrderByCreatedAtAsc(Long documentId);

    // 根据文档ID和位置查找评论
    List<Comment> findByDocumentIdAndPositionOrderByCreatedAtAsc(Long documentId, Integer position);

    // 查找未解决的评论
    List<Comment> findByDocumentIdAndResolvedFalseOrderByCreatedAtAsc(Long documentId);

    // 查找用户的评论
    List<Comment> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 根据父评论ID查找回复
    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);

    // 查找文档中提及指定用户的评论
    @Query("SELECT c FROM Comment c JOIN c.mentionedUsers mu WHERE c.document.id = :documentId AND mu.id = :userId ORDER BY c.createdAt DESC")
    List<Comment> findMentionsInDocument(@Param("documentId") Long documentId, @Param("userId") Long userId);

    // 标记评论为已解决
    @Modifying
    @Query("UPDATE Comment c SET c.resolved = true, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :commentId")
    void markAsResolved(@Param("commentId") Long commentId);

    // 统计文档的未解决评论数量
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.document.id = :documentId AND c.resolved = false")
    Long countUnresolvedComments(@Param("documentId") Long documentId);

    // 删除文档的所有评论
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);
}