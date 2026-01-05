package com.example.multiuser_online_editing.service.document_management;

import com.example.multiuser_online_editing.entity.document_management.DocumentType;
import com.example.multiuser_online_editing.entity.document_management.Template;
import com.example.multiuser_online_editing.entity.document_management.TemplateCategory;
import com.example.multiuser_online_editing.entity.user_management.OperationLog;
import com.example.multiuser_online_editing.entity.user_management.Role;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.example.multiuser_online_editing.repository.document_management.TemplateRepository;
import com.example.multiuser_online_editing.repository.user_management.OperationLogRepository;
import com.example.multiuser_online_editing.repository.user_management.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class TemplateService {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    // 创建模板
    public Template createTemplate(String name, String description, String content,
                                   TemplateCategory category, Boolean isPublic,
                                   DocumentType documentType, User owner) {
        if (owner.getRole() == Role.VIEWER) {
            throw new RuntimeException("您当前的角色为查看者，无法创建模板");
        }

        Template template = new Template();
        template.setName(name);
        template.setDescription(description);
        template.setContent(content);
        template.setCategory(category);
        template.setIsPublic(isPublic != null ? isPublic : false);
        template.setDocumentType(documentType);
        template.setOwner(owner);

        Template savedTemplate = templateRepository.save(template);

        // 记录操作日志
        logOperation(owner.getId(), "CREATE_TEMPLATE", "TEMPLATE", savedTemplate.getId(),
                "创建模板: " + name + (isPublic ? " (公开)" : " (私有)"));

        return savedTemplate;
    }

    // 更新模板
    public Template updateTemplate(Long templateId, String name, String description, String content,
                                   TemplateCategory category, Boolean isPublic, User user) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在"));

        // 检查权限
        if (!template.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("无权修改此模板");
        }

        if (name != null) template.setName(name);
        if (description != null) template.setDescription(description);
        if (content != null) template.setContent(content);
        if (category != null) template.setCategory(category);
        if (isPublic != null) template.setIsPublic(isPublic);

        template.setUpdatedAt(LocalDateTime.now());

        Template updatedTemplate = templateRepository.save(template);

        // 记录操作日志
        logOperation(user.getId(), "UPDATE_TEMPLATE", "TEMPLATE", templateId,
                "更新模板: " + name);

        return updatedTemplate;
    }

    // 删除模板
    public void deleteTemplate(Long templateId, User user) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在"));

        // 检查权限
        if (!template.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("无权删除此模板");
        }

        templateRepository.delete(template);

        // 记录操作日志
        logOperation(user.getId(), "DELETE_TEMPLATE", "TEMPLATE", templateId,
                "删除模板: " + template.getName());
    }

    // 获取用户可用的模板（自己的模板 + 公开模板）
    public Page<Template> getAvailableTemplates(User user, Pageable pageable) {
        return templateRepository.findByOwnerOrIsPublicTrueOrderByUpdatedAtDesc(user, pageable);
    }

    // 获取用户的私有模板
    public List<Template> getUserTemplates(User user) {
        return templateRepository.findByOwnerOrderByName(user);
    }

    // 获取公开模板（按分类）
    public List<Template> getPublicTemplatesByCategory(TemplateCategory category) {
        return templateRepository.findByCategoryAndIsPublicTrue(category);
    }

    // 获取模板详情
    public Template getTemplateDetail(Long templateId, User user) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在"));

        // 检查权限（公开模板或自己的模板）
        if (!template.getIsPublic() && !template.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("无权查看此模板");
        }

        return template;
    }

    // 切换模板公开状态
    public Template toggleTemplateVisibility(Long templateId, User user) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在"));

        // 检查权限
        if (!template.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("无权修改此模板的公开状态");
        }

        template.setIsPublic(!template.getIsPublic()); // 与原来的状态相反
        Template updatedTemplate = templateRepository.save(template);

        // 记录操作日志
        logOperation(user.getId(), "TOGGLE_TEMPLATE_VISIBILITY", "TEMPLATE", templateId,
                (updatedTemplate.getIsPublic() ? "公开" : "私有") + "模板: " + template.getName());

        return updatedTemplate;
    }

    private void logOperation(Long userId, String operation, String resourceType, Long resourceId, String details) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            OperationLog log =
                    new OperationLog(user, operation, resourceType, resourceId);
            log.setDetails(details);
            operationLogRepository.save(log);
        }
    }
}