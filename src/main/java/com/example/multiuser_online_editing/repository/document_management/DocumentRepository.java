package com.example.multiuser_online_editing.repository.document_management;

import com.example.multiuser_online_editing.entity.document_management.Document;
import com.example.multiuser_online_editing.entity.document_management.DocumentStatus;
import com.example.multiuser_online_editing.entity.document_management.Folder;
import com.example.multiuser_online_editing.entity.user_management.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByOwnerAndId(User owner, Long id);

    // 根据所有者查找文档
    Page<Document> findByOwnerAndStatusNotOrderByUpdatedAtDesc(User owner, DocumentStatus status, Pageable pageable);
    List<Document> findByOwnerAndStatusNot(User owner, DocumentStatus status);

    // 根据所有者查找根目录下的文档（parent为null）
    Page<Document> findByOwnerAndFolderIsNullAndStatusNotOrderByUpdatedAtDesc(User owner, DocumentStatus status, Pageable pageable);
    List<Document> findByOwnerAndFolderIsNullAndStatusNot(User owner, DocumentStatus status);

    // 高级搜索
    @Query("SELECT d FROM Document d LEFT JOIN d.tags t WHERE " +
            "(:title IS NULL OR d.title LIKE %:title%) AND " +
            "(:content IS NULL OR d.content LIKE %:content%) AND " +
            "(:startDate IS NULL OR d.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR d.createdAt <= :endDate) AND " +
            "(:tagName IS NULL OR t.name = :tagName) AND " +
            "d.status != 'DELETED' AND " +
            "(d.owner = :currentUser) " +
            "ORDER BY d.updatedAt DESC")
    Page<Document> advancedSearch_isOwner(@Param("title") String title,
                                  @Param("content") String content,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate,
                                  @Param("currentUser") User currentUser,
                                  @Param("tagName") String tagName,
                                  Pageable pageable);

    @Query("SELECT d FROM Document d LEFT JOIN d.tags t WHERE " +
            "(:title IS NULL OR d.title LIKE %:title%) AND " +
            "(:content IS NULL OR d.content LIKE %:content%) AND " +
            "(:owner IS NULL OR d.owner = :owner) AND " +
            "(:startDate IS NULL OR d.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR d.createdAt <= :endDate) AND " +
            "(:tagName IS NULL OR t.name = :tagName) AND " +
            "d.status != 'DELETED' AND " +
            "(:currentUser MEMBER OF d.collaborators)" +
            "ORDER BY d.updatedAt DESC")
    Page<Document> advancedSearch_isCollaborator(@Param("title") String title,
                                  @Param("content") String content,
                                  @Param("owner") User owner,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate,
                                  @Param("currentUser") User currentUser,
                                  @Param("tagName") String tagName,
                                  Pageable pageable);

    List<Document> findByOwnerAndFolderAndStatusNot(User owner, Folder folder, DocumentStatus status);

    List<Document> findByOwnerAndStatus(User owner, DocumentStatus status);

    // 根据文档的ID搜索文档
    Optional<Document> findByIdAndStatusNot(Long id, DocumentStatus status);

    // 搜索某个用户可以访问的（作为所有者或协作者的文档）
    @Query("SELECT d FROM Document d WHERE " +
            "d.status != 'DELETED' AND " +
            "(d.owner.id = :userId OR :userId IN (SELECT c.id FROM d.collaborators c)) " +
            "ORDER BY d.updatedAt DESC")
    List<Document> findAccessibleDocuments(@Param("userId") Long userId);

}