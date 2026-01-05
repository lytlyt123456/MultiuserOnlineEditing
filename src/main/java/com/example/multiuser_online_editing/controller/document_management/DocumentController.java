package com.example.multiuser_online_editing.controller.document_management;

import com.example.multiuser_online_editing.controller.ApiResponse;
import com.example.multiuser_online_editing.entity.document_management.Document;
import com.example.multiuser_online_editing.entity.document_management.DocumentType;
import com.example.multiuser_online_editing.entity.document_management.Tag;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.example.multiuser_online_editing.service.document_management.DocumentClass;
import com.example.multiuser_online_editing.service.document_management.DocumentService;
import com.example.multiuser_online_editing.service.user_management.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    public static Map<String, Object> documentToMap(Document document) {
        Map<String, Object> document_res = new HashMap<>();
        document_res.put("id", document.getId());
        document_res.put("title", document.getTitle());
        document_res.put("content", document.getContent());
        document_res.put("owner", document.getOwner().getUsername());
        document_res.put("status", document.getStatus());
        document_res.put("type", document.getType());
        document_res.put("createdAt", document.getCreatedAt());
        document_res.put("updatedAt", document.getUpdatedAt());
        document_res.put("version", document.getVersion());
        document_res.put("autoSaveContent", document.getAutoSaveContent());
        document_res.put("autoSaveTime", document.getAutoSaveTime());

        List<User> collaborators = document.getCollaborators();
        List<String> collaboratorsUsername = new ArrayList<>();
        for (User collaborator: collaborators)
            collaboratorsUsername.add(collaborator.getUsername());
        document_res.put("collaborators", collaboratorsUsername);

        List<Tag> tags = document.getTags();
        List<String> tagNames = new ArrayList<>();
        for (Tag tag: tags)
            tagNames.add(tag.getName());
        document_res.put("tags", tagNames);

        if (document.getFolder() != null)
            document_res.put("folder", document.getFolder().toString());
        else document_res.put("folder", "根目录");

        return document_res;
    }

    public static List<Map<String, Object>> documentsToMaps(List<Document> documents) {
        List<Map<String, Object>> documents_res = new ArrayList<>();
        for (Document document : documents)
            documents_res.add(documentToMap(document));
        return documents_res;
    }

    public static List<Map<String, Object>> documentsToMaps(Page<Document> documents) {
        return documentsToMaps(documents.getContent());
    }

    @Autowired
    private DocumentService documentService;

    @Autowired
    private UserService userService;

    // 创建文档
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createDocument(
            @RequestBody CreateDocumentRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Document document = documentService.createDocument(
                    request.getTitle(),
                    request.getContent(),
                    request.getType(),
                    request.getFolderId(),
                    request.getTagNames(),
                    currentUser
            );

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("documentId", document.getId());
            responseData.put("title", document.getTitle());
            responseData.put("type", document.getType());

            return ResponseEntity.ok(ApiResponse.success("文档创建成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 使用模板创建文档
    @PostMapping("/from-template")
    public ResponseEntity<ApiResponse<Object>> createDocumentFromTemplate(
            @RequestBody CreateDocumentFromTemplateRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Document document = documentService.createDocumentFromTemplate(
                    request.getTitle(),
                    request.getTemplateId(),
                    request.getType(),
                    request.getFolderId(),
                    currentUser,
                    request.getTagNames()
            );

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("documentId", document.getId());
            responseData.put("title", document.getTitle());
            responseData.put("type", document.getType());

            return ResponseEntity.ok(ApiResponse.success("使用模板创建文档成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 更新文档
    @PutMapping("/{documentId}")
    public ResponseEntity<ApiResponse<Object>> updateDocument(
            @PathVariable Long documentId,
            @RequestBody UpdateDocumentRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Document document = documentService.updateDocument(
                    documentId,
                    request.getTitle(),
                    request.getContent(),
                    currentUser
            );

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("documentId", document.getId());
            responseData.put("title", document.getTitle());
            responseData.put("version", document.getVersion());

            return ResponseEntity.ok(ApiResponse.success("文档更新成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 自动保存文档
    @PostMapping("/{documentId}/auto-save")
    public ResponseEntity<ApiResponse<Object>> autoSaveDocument(
            @PathVariable Long documentId,
            @RequestBody AutoSaveRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            documentService.autoSaveDocument(documentId, request.getContent(), currentUser);

            return ResponseEntity.ok(ApiResponse.success("自动保存成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 恢复自动保存内容
    @GetMapping("/{documentId}/restore-auto-save")
    public ResponseEntity<ApiResponse<Object>> restoreAutoSaveContent(@PathVariable Long documentId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            String content = documentService.restoreAutoSaveContent(documentId, currentUser);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("content", content);

            return ResponseEntity.ok(ApiResponse.success("恢复自动保存内容成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 高级搜索
    @GetMapping("/advanced-search-owner")
    public ResponseEntity<ApiResponse<Object>> advancedSearch_isOwner(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String tagName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
            Page<Document> documents = documentService.advancedSearch_isOwner(
                    title, content, startDate, endDate, currentUser, tagName, pageable);

            List<Map<String,Object>> documents_res = documentsToMaps(documents);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("documents", documents_res);
            responseData.put("totalPages", documents.getTotalPages());
            responseData.put("totalElements", documents.getTotalElements());
            responseData.put("currentPage", documents.getNumber());

            return ResponseEntity.ok(ApiResponse.success("高级搜索成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 高级搜索
    @GetMapping("/advanced-search-collaborator")
    public ResponseEntity<ApiResponse<Object>> advancedSearch_isCollaborator(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String ownerUsername,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String tagName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
            Page<Document> documents = documentService.advancedSearch_isCollaborator(
                    title, content, ownerUsername, startDate, endDate, currentUser, tagName, pageable);

            List<Map<String,Object>> documents_res = documentsToMaps(documents);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("documents", documents_res);
            responseData.put("totalPages", documents.getTotalPages());
            responseData.put("totalElements", documents.getTotalElements());
            responseData.put("currentPage", documents.getNumber());

            return ResponseEntity.ok(ApiResponse.success("高级搜索成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/advanced-search-ai")
    public ResponseEntity<ApiResponse<Object>> advancedSearchAI(
            @RequestParam String content,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Long currentUserId = userService.getCurrentUserId();

            // 调用AI搜索服务
            List<Document> documents = documentService.advancedSearch_AI(content, currentUserId);
            List<Map<String, Object>> documents_res = documentsToMaps(documents);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("documents", documents_res);

            return ResponseEntity.ok(ApiResponse.success("AI智能搜索成功", responseData));

        } catch (RuntimeException e) {
            System.err.println("AI搜索错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error("搜索失败: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("AI搜索系统错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("系统错误，请稍后重试"));
        }
    }

    @GetMapping("/document-clustering")
    public ResponseEntity<ApiResponse<Object>> documentClustering(
            @RequestParam(defaultValue = "3") int k) {
        try {
            Long currentUserId = userService.getCurrentUserId();

            // 调用文档聚类服务
            List<DocumentClass> documentClasses = documentService.documentClustering(currentUserId, k);

            // 转换为前端需要的格式
            List<Map<String, Object>> classesData = new ArrayList<>();
            for (DocumentClass docClass : documentClasses) {
                Map<String, Object> classData = new HashMap<>();
                classData.put("themeWords", docClass.getThemeWords());
                classData.put("documents", documentsToMaps(docClass.getDocuments()));
                classesData.add(classData);
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("classes", classesData);
            responseData.put("totalClasses", documentClasses.size());

            return ResponseEntity.ok(ApiResponse.success("文档聚类成功", responseData));

        } catch (RuntimeException e) {
            System.err.println("文档聚类错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error("聚类失败: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("文档聚类系统错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("系统错误，请稍后重试"));
        }
    }

    // 添加协作者
    @PostMapping("/{documentId}/collaborators")
    public ResponseEntity<ApiResponse<Object>> addCollaborator(
            @PathVariable Long documentId,
            @RequestBody AddCollaboratorRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Document document = documentService.addCollaborator(
                    documentId, request.getUserId(), currentUser);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("documentId", document.getId());
            responseData.put("collaboratorsCount", document.getCollaborators().size());

            return ResponseEntity.ok(ApiResponse.success("添加协作者成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 删除文档（软删除）
    @DeleteMapping("/{documentId}")
    public ResponseEntity<ApiResponse<Object>> deleteDocument(@PathVariable Long documentId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            documentService.deleteDocument(documentId, currentUser);

            return ResponseEntity.ok(ApiResponse.success("文档删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 获取文档详情
    @GetMapping("/{documentId}")
    public ResponseEntity<ApiResponse<Object>> getDocumentDetail(@PathVariable Long documentId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Document document = documentService.getDocumentDetail(documentId, currentUser);

            // 构建响应数据
            Map<String, Object> documentInfo = documentToMap(document);

            return ResponseEntity.ok(ApiResponse.success("获取文档详情成功", documentInfo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 移除协作者
    @DeleteMapping("/{documentId}/collaborators/{userId}")
    public ResponseEntity<ApiResponse<Object>> removeCollaborator(
            @PathVariable Long documentId,
            @PathVariable Long userId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Document document = documentService.removeCollaborator(documentId, userId, currentUser);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("documentId", document.getId());
            responseData.put("collaboratorsCount", document.getCollaborators().size());

            return ResponseEntity.ok(ApiResponse.success("移除协作者成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/root-documents")
    public ResponseEntity<ApiResponse<Object>> getRootDocuments() {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            List<Document> documents = documentService.getRootDocuments(currentUser);

            List<Map<String,Object>> documents_res = documentsToMaps(documents);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("documents", documents_res);

            return ResponseEntity.ok(ApiResponse.success("获取根目录文档成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/recycle-bin")
    public ResponseEntity<ApiResponse<Object>> getDeletedDocuments() {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            List<Document> documents = documentService.getDeletedDocuments(currentUser);

            List<Map<String, Object>> documents_res = documentsToMaps(documents);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("documents", documents_res);

            return ResponseEntity.ok(ApiResponse.success("获取已删除的文档成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{documentId}/del-forever")
    public ResponseEntity<ApiResponse<Object>> deleteDocumentForever(@PathVariable Long documentId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);
            documentService.deleteDocumentForever(documentId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("文档删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{documentId}/restore-document")
    public ResponseEntity<ApiResponse<Object>> restoreDocument(@PathVariable Long documentId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);
            documentService.restoreDocument(documentId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("文档恢复成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

// 请求DTO类
class CreateDocumentRequest {
    private String title;
    private String content;
    private DocumentType type;
    private Long folderId;
    private List<String> tagNames;

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public DocumentType getType() { return type; }
    public void setType(DocumentType type) { this.type = type; }
    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }
    public List<String> getTagNames() { return tagNames; }
    public void setTagNames(List<String> tagNames) { this.tagNames = tagNames; }
}

class CreateDocumentFromTemplateRequest {
    private String title;
    private Long templateId;
    private DocumentType type;
    private Long folderId;
    private List<String> tagNames;

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public DocumentType getType() { return type; }
    public void setType(DocumentType type) { this.type = type; }
    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }
    public List<String> getTagNames() { return tagNames; }
    public void setTagNames(List<String> tagNames) { this.tagNames = tagNames; }
}

class UpdateDocumentRequest {
    private String title;
    private String content;

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}

class AutoSaveRequest {
    private String content;

    // Getters and Setters
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}

class AddCollaboratorRequest {
    private Long userId;

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
