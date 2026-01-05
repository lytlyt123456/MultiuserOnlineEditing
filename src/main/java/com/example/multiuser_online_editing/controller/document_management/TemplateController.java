package com.example.multiuser_online_editing.controller.document_management;

import com.example.multiuser_online_editing.controller.ApiResponse;
import com.example.multiuser_online_editing.entity.document_management.DocumentType;
import com.example.multiuser_online_editing.entity.document_management.Template;
import com.example.multiuser_online_editing.entity.document_management.TemplateCategory;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.example.multiuser_online_editing.service.document_management.TemplateService;
import com.example.multiuser_online_editing.service.user_management.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    @Autowired
    private UserService userService;

    public static Map<String, Object> templateToMap(Template template) {
        Map<String, Object> template_res = new HashMap<>();
        template_res.put("id", template.getId());
        template_res.put("name", template.getName());
        template_res.put("description", template.getDescription());
        template_res.put("content", template.getContent());
        template_res.put("category", template.getCategory());
        template_res.put("owner_id", template.getOwner().getId());
        template_res.put("is_public", template.getIsPublic());
        template_res.put("created_at", template.getCreatedAt());
        template_res.put("updated_at", template.getUpdatedAt());
        template_res.put("document_type", template.getDocumentType());

        return template_res;
    }

    public static List<Map<String, Object>> templatesToMaps(List<Template> templates) {
        List<Map<String, Object>> templates_res = new ArrayList<>();
        for (Template template : templates)
            templates_res.add(templateToMap(template));
        return templates_res;
    }

    public static List<Map<String, Object>> templatesToMaps(Page<Template> templates) {
        return templatesToMaps(templates.getContent());
    }

    // 创建模板
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createTemplate(@RequestBody CreateTemplateRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Template template = templateService.createTemplate(
                    request.getName(),
                    request.getDescription(),
                    request.getContent(),
                    request.getCategory(),
                    request.getIsPublic(),
                    request.getDocumentType(),
                    currentUser
            );

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("templateId", template.getId());
            responseData.put("name", template.getName());
            responseData.put("isPublic", template.getIsPublic());

            return ResponseEntity.ok(ApiResponse.success("模板创建成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 更新模板
    @PutMapping("/{templateId}")
    public ResponseEntity<ApiResponse<Object>> updateTemplate(
            @PathVariable Long templateId,
            @RequestBody UpdateTemplateRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Template template = templateService.updateTemplate(
                    templateId,
                    request.getName(),
                    request.getDescription(),
                    request.getContent(),
                    request.getCategory(),
                    request.getIsPublic(),
                    currentUser
            );

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("templateId", template.getId());
            responseData.put("name", template.getName());

            return ResponseEntity.ok(ApiResponse.success("模板更新成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 删除模板
    @DeleteMapping("/{templateId}")
    public ResponseEntity<ApiResponse<Object>> deleteTemplate(@PathVariable Long templateId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            templateService.deleteTemplate(templateId, currentUser);

            return ResponseEntity.ok(ApiResponse.success("模板删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 获取可用模板列表（自己的模板 + 公开模板）
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getAvailableTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
            Page<Template> templates = templateService.getAvailableTemplates(currentUser, pageable);

            List<Map<String, Object>> templates_res = templatesToMaps(templates);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("templates", templates_res);
            responseData.put("totalPages", templates.getTotalPages());
            responseData.put("totalElements", templates.getTotalElements());
            responseData.put("currentPage", templates.getNumber());

            return ResponseEntity.ok(ApiResponse.success("获取模板列表成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 获取用户的私有模板
    @GetMapping("/my-templates")
    public ResponseEntity<ApiResponse<Object>> getUserTemplates() {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            List<Template> templates = templateService.getUserTemplates(currentUser);

            List<Map<String, Object>> templates_res = templatesToMaps(templates);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("templates", templates_res);

            return ResponseEntity.ok(ApiResponse.success("获取用户模板列表成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 按分类获取公开模板
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<Object>> getPublicTemplatesByCategory(
            @RequestParam TemplateCategory category) {
        try {
            List<Template> templates = templateService.getPublicTemplatesByCategory(category);

            List<Map<String, Object>> templates_res = templatesToMaps(templates);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("templates", templates_res);

            return ResponseEntity.ok(ApiResponse.success("获取公开模板成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 获取模板详情
    @GetMapping("/{templateId}")
    public ResponseEntity<ApiResponse<Object>> getTemplateDetail(@PathVariable Long templateId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Template template = templateService.getTemplateDetail(templateId, currentUser);

            Map<String, Object> template_res = templateToMap(template);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("template", template_res);

            return ResponseEntity.ok(ApiResponse.success("获取模板详情成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 切换模板公开状态
    @PutMapping("/{templateId}/toggle-visibility")
    public ResponseEntity<ApiResponse<Object>> toggleTemplateVisibility(@PathVariable Long templateId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Template template = templateService.toggleTemplateVisibility(templateId, currentUser);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("templateId", template.getId());
            responseData.put("name", template.getName());
            responseData.put("isPublic", template.getIsPublic());

            return ResponseEntity.ok(ApiResponse.success(
                    template.getIsPublic() ? "模板已设为公开" : "模板已设为私有",
                    responseData
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

// 请求DTO类
class CreateTemplateRequest {
    private String name;
    private String description;
    private String content;
    private TemplateCategory category;
    private Boolean isPublic;
    public DocumentType documentType;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public TemplateCategory getCategory() { return category; }
    public void setCategory(TemplateCategory category) { this.category = category; }
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }
}

class UpdateTemplateRequest {
    private String name;
    private String description;
    private String content;
    private TemplateCategory category;
    private Boolean isPublic;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public TemplateCategory getCategory() { return category; }
    public void setCategory(TemplateCategory category) { this.category = category; }
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
}

class CopyTemplateRequest {
    private String newName;

    // Getters and Setters
    public String getNewName() { return newName; }
    public void setNewName(String newName) { this.newName = newName; }
}