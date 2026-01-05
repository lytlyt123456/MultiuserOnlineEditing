package com.example.multiuser_online_editing.service.document_management;

import com.example.multiuser_online_editing.entity.document_management.*;
import com.example.multiuser_online_editing.entity.user_management.OperationLog;
import com.example.multiuser_online_editing.entity.user_management.Role;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.example.multiuser_online_editing.repository.document_management.DocumentRepository;
import com.example.multiuser_online_editing.repository.document_management.FolderRepository;
import com.example.multiuser_online_editing.repository.document_management.TagRepository;
import com.example.multiuser_online_editing.repository.document_management.TemplateRepository;
import com.example.multiuser_online_editing.repository.user_management.OperationLogRepository;
import com.example.multiuser_online_editing.repository.user_management.UserRepository;
import com.example.multiuser_online_editing.service.communication.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private NotificationService notificationService;

    // 创建文档
    public Document createDocument(String title, String content, DocumentType type,
                                   Long folderId, List<String> tagNames, User owner) {
        if (owner.getRole() == Role.VIEWER)
            throw new RuntimeException("您当前的角色为查看者，请向系统管理员申请升级为编辑者，并在申请通过后，再进行文件创建");

        Document document = new Document();
        document.setTitle(title);
        document.setContent(content);
        document.setType(type);
        document.setOwner(owner);
        document.setStatus(DocumentStatus.EXISTS);

        // 设置文件夹（必须保证文件夹归这个创建文件的用户所有）
        if (folderId != null) {
            Folder folder = folderRepository.findByIdAndOwner(folderId, owner)
                    .orElseThrow(() -> new RuntimeException("文件夹不存在或无权访问"));
            document.setFolder(folder);
        }

        // 设置标签
        if (tagNames != null && !tagNames.isEmpty()) {
            List<Tag> tags = new ArrayList<>();
            for (String tagName : tagNames) {
                Tag tag = tagRepository.findByNameAndOwner(tagName, owner) // 必须保证标签归用户所有
                        .orElseGet(() -> { // 如果没有该标签，则创建一个新的标签
                            Tag newTag = new Tag();
                            newTag.setName(tagName);
                            newTag.setOwner(owner);
                            return tagRepository.save(newTag);
                        });
                tags.add(tag);
            }
            document.setTags(tags);
        }

        Document savedDocument = documentRepository.save(document);

        // 记录操作日志
        logOperation(owner.getId(), "CREATE_DOCUMENT", "DOCUMENT", savedDocument.getId(),
                "创建" + type + "文档: " + title);

        return savedDocument;
    }

    // 使用模板创建文档（模板的content将作为文档的content）
    public Document createDocumentFromTemplate(String title, Long templateId,
                                               DocumentType type, Long folderId, User owner, List<String> tagNames) {
        if (owner.getRole() == Role.VIEWER)
            throw new RuntimeException("您当前的角色为查看者，请向系统管理员申请升级为编辑者，并在申请通过后，再进行文件创建");

        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在"));

        // 检查模板权限
        if (!template.getIsPublic() && !template.getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException("无权使用此模板");
        }

        Document document = createDocument(title, template.getContent(), type, folderId, tagNames, owner);

        // 记录操作日志
        logOperation(owner.getId(), "CREATE_DOCUMENT_FROM_TEMPLATE", "DOCUMENT", document.getId(),
                "使用模板创建文档: " + title + ", 模板: " + template.getName());

        return document;
    }

    // 更新文档内容
    public Document updateDocument(Long documentId, String title, String content, User user) {
        if (user.getRole() == Role.VIEWER)
            throw new RuntimeException("您当前的角色为查看者，请向系统管理员申请升级为编辑者，并在申请通过后，再进行文档编辑");

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        // 检查权限：所有者或协作者可以编辑
        if (!document.getOwner().getId().equals(user.getId()) &&
                document.getCollaborators().stream().noneMatch(c -> c.getId().equals(user.getId()))) {
            throw new RuntimeException("无权编辑此文档");
        }

        if (title != null) document.setTitle(title);
        if (content != null) {
            document.setContent(content);
            // 清空自动保存中的内容
            document.setAutoSaveContent(null);
            document.setAutoSaveTime(null);
        }
        document.setVersion(document.getVersion() + 1);

        Document updatedDocument = documentRepository.save(document);

        // 记录操作日志
        logOperation(user.getId(), "UPDATE_DOCUMENT", "DOCUMENT", documentId,
                "更新文档: " + document.getTitle());

        return updatedDocument;
    }

    // 自动保存
    public void autoSaveDocument(Long documentId, String content, User user) {
        if (user.getRole() == Role.VIEWER)
            throw new RuntimeException("您当前的角色为查看者，请向系统管理员申请升级为编辑者，并在申请通过后，再进行文档编辑");

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        // 检查权限
        if (!document.getOwner().getId().equals(user.getId()) &&
                document.getCollaborators().stream().noneMatch(c -> c.getId().equals(user.getId()))) {
            throw new RuntimeException("无权编辑此文档");
        }

        document.setAutoSaveContent(content);
        document.setAutoSaveTime(LocalDateTime.now());
        documentRepository.save(document);
    }

    // 恢复自动保存内容
    public String restoreAutoSaveContent(Long documentId, User user) {
        if (user.getRole() == Role.VIEWER)
            throw new RuntimeException("您当前的角色为查看者，请向系统管理员申请升级为编辑者，并在申请通过后，再进行文档编辑");

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        // 检查权限
        if (!document.getOwner().getId().equals(user.getId()) &&
                document.getCollaborators().stream().noneMatch(c -> c.getId().equals(user.getId()))) {
            throw new RuntimeException("无权访问此文档");
        }

        if (document.getAutoSaveContent() == null) {
            throw new RuntimeException("没有自动保存的内容");
        }

        return document.getAutoSaveContent();
    }

    // 高级搜索
    public Page<Document> advancedSearch_isOwner(String title, String content,
                                         LocalDateTime startDate, LocalDateTime endDate,
                                         User currentUser, String tagName, Pageable pageable) {

//        System.out.println("title: " + title);
//        System.out.println("content: " + content);
//        System.out.println("currentUser: " + currentUser.getUsername());
//        System.out.println("startDate: " + startDate);
//        System.out.println("endDate: " + endDate);

        return documentRepository.advancedSearch_isOwner(title, content, startDate, endDate,
                currentUser, tagName, pageable);
    }

    public Page<Document> advancedSearch_isCollaborator(String title, String content, String ownerUsername,
                                                 LocalDateTime startDate, LocalDateTime endDate,
                                                 User currentUser, String tagName, Pageable pageable) {

        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new RuntimeException("文档所有者用户名不存在"));

        return documentRepository.advancedSearch_isCollaborator(title, content, owner, startDate, endDate, currentUser,
                tagName, pageable);
    }

    // AI搜索与用户相关的所有文档
    public List<Document> advancedSearch_AI(String content, Long userId) {
        List<Document> accessibleDocuments = documentRepository.findAccessibleDocuments(userId);
        return DocumentSearch_AI.advancedSearch_AI(content, accessibleDocuments);
    }

    // 聚类算法对文档进行智能分类
    public List<DocumentClass> documentClustering(Long userId, int numOfClusters) {
        List<Document> accessibleDocuments = documentRepository.findAccessibleDocuments(userId);
        return Classification_AI.classification_AI(accessibleDocuments, numOfClusters);
    }

    // 添加协作者
    public Document addCollaborator(Long documentId, Long userId, User currentUser) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        // 只有所有者可以添加协作者
        if (!document.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("只有文档所有者可以添加协作者");
        }

        User collaborator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (collaborator.getUsername().equals(currentUser.getUsername())) {
            throw new RuntimeException("不能添加自己为协作者");
        }

        if (document.getCollaborators().stream().anyMatch(c -> c.getId().equals(userId))) {
            throw new RuntimeException("用户已是协作者");
        }

        document.getCollaborators().add(collaborator);
        Document updatedDocument = documentRepository.save(document);

        // 记录操作日志
        logOperation(currentUser.getId(), "ADD_COLLABORATOR", "DOCUMENT", documentId,
                "添加协作者: " + collaborator.getUsername());

        String message = currentUser.getUsername() + " 添加你为文档 \"" + document.getTitle() + "\" 的协作者";
        notificationService.sendNotification(collaborator.getId(), "添加协作者", message);

        return updatedDocument;
    }

    // 删除文档（软删除）
    public void deleteDocument(Long documentId, User user) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        // 只有所有者可以删除
        if (!document.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("只有文档所有者可以删除文档");
        }

        document.setStatus(DocumentStatus.DELETED);
        documentRepository.save(document);

        // 记录操作日志
        logOperation(user.getId(), "DELETE_DOCUMENT", "DOCUMENT", documentId,
                "删除文档: " + document.getTitle());
    }

    // 获取文档详情
    public Document getDocumentDetail(Long documentId, User user) {
        Document document = documentRepository.findByIdAndStatusNot(documentId, DocumentStatus.DELETED)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        // 检查权限：所有者、协作者或管理员可以查看
        boolean hasPermission = document.getOwner().getId().equals(user.getId()) ||
                document.getCollaborators().stream().anyMatch(c -> c.getId().equals(user.getId()));

        if (!hasPermission) {
            throw new RuntimeException("无权查看此文档");
        }

        documentRepository.save(document);

        // 记录操作日志
        logOperation(user.getId(), "VIEW_DOCUMENT", "DOCUMENT", documentId,
                "查看文档: " + document.getTitle());

        return document;
    }

    // 移除协作者
    public Document removeCollaborator(Long documentId, Long userId, User currentUser) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        // 只有所有者可以移除协作者
        if (!document.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("只有文档所有者可以移除协作者");
        }

        User collaborator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查是否是协作者
        boolean isCollaborator = document.getCollaborators().stream()
                .anyMatch(c -> c.getId().equals(userId));

        if (!isCollaborator) {
            throw new RuntimeException("该用户不是文档协作者");
        }

        // 移除协作者
        document.getCollaborators().removeIf(c -> c.getId().equals(userId));
        Document updatedDocument = documentRepository.save(document);

        // 记录操作日志
        logOperation(currentUser.getId(), "REMOVE_COLLABORATOR", "DOCUMENT", documentId,
                "移除协作者: " + collaborator.getUsername());

        String message = currentUser.getUsername() + " 将你从文档 \"" + document.getTitle() + "\" 的协作者中移除";
        notificationService.sendNotification(collaborator.getId(), "移除协作者", message);

        return updatedDocument;
    }

    // 获取根目录下的文档
    public List<Document> getRootDocuments(User user) {
        return documentRepository.findByOwnerAndFolderIsNullAndStatusNot(user, DocumentStatus.DELETED);
    }

    // 获取根目录下的文档（分页）
    public Page<Document> getRootDocuments(User user, Pageable pageable) {
        return documentRepository.findByOwnerAndFolderIsNullAndStatusNotOrderByUpdatedAtDesc(user, DocumentStatus.DELETED, pageable);
    }

    public List<Document> getDeletedDocuments(User user) {
        return documentRepository.findByOwnerAndStatus(user, DocumentStatus.DELETED);
    }

    public void deleteDocumentForever(Long documentId, User user) {
        Document document = documentRepository.findByOwnerAndId(user, documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        if (document.getStatus() != DocumentStatus.DELETED)
            throw new RuntimeException("未放入回收站的文档不能永久删除");

        documentRepository.delete(document);

        logOperation(user.getId(), "DELETE_DOCUMENT_FOREVER", "DOCUMENT", documentId,
                "永久删除文档: " + document.getTitle());
    }

    public void restoreDocument(Long documentId, User user) {
        Document document = documentRepository.findByOwnerAndId(user, documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        if (document.getStatus() != DocumentStatus.DELETED)
            throw new RuntimeException("文档已存在，无需恢复");

        document.setStatus(DocumentStatus.EXISTS);

        logOperation(user.getId(), "RESTORE_DOCUMENT", "DOCUMENT", documentId,
                "恢复文档: " + document.getTitle());
    }

    private void logOperation(Long userId, String operation, String resourceType, Long resourceId, String details) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            OperationLog log = new OperationLog(user, operation, resourceType, resourceId);
            log.setDetails(details);
            operationLogRepository.save(log);
        }
    }
}