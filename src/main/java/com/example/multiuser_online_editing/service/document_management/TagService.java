package com.example.multiuser_online_editing.service.document_management;

import com.example.multiuser_online_editing.entity.document_management.Tag;
import com.example.multiuser_online_editing.entity.user_management.OperationLog;
import com.example.multiuser_online_editing.entity.user_management.Role;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.example.multiuser_online_editing.repository.document_management.TagRepository;
import com.example.multiuser_online_editing.repository.user_management.OperationLogRepository;
import com.example.multiuser_online_editing.repository.user_management.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TagService {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    // 创建标签
    public Tag createTag(String name, String description, User owner) {
        if (owner.getRole() == Role.VIEWER) {
            throw new RuntimeException("您当前的角色为查看者，无法创建标签");
        }

        // 检查标签名是否已存在（对同一用户）
        Optional<Tag> existingTag = tagRepository.findByNameAndOwner(name, owner);
        if (existingTag.isPresent()) {
            throw new RuntimeException("标签名称已存在");
        }

        Tag tag = new Tag();
        tag.setName(name);
        tag.setDescription(description);
        tag.setOwner(owner);

        Tag savedTag = tagRepository.save(tag);

        // 记录操作日志
        logOperation(owner.getId(), "CREATE_TAG", "TAG", savedTag.getId(),
                "创建标签: " + name);

        return savedTag;
    }

    // 更新标签
    public Tag updateTag(Long tagId, String name, String description, User user) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        // 检查权限
        if (!tag.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("无权修改此标签");
        }

        // 检查标签名是否已存在（排除自身）
        if (!tag.getName().equals(name)) {
            Optional<Tag> existingTag = tagRepository.findByNameAndOwner(name, user);
            if (existingTag.isPresent()) {
                throw new RuntimeException("标签名称已存在");
            }
        }

        if (name != null) tag.setName(name);
        if (description != null) tag.setDescription(description);

        Tag updatedTag = tagRepository.save(tag);

        // 记录操作日志
        logOperation(user.getId(), "UPDATE_TAG", "TAG", tagId,
                "更新标签: " + name);

        return updatedTag;
    }

    // 删除标签
    public void deleteTag(Long tagId, User user) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        // 检查权限
        if (!tag.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("无权删除此标签");
        }

        // 检查标签是否被文档使用
        if (!tag.getDocuments().isEmpty()) {
            throw new RuntimeException("标签正在被文档使用，无法删除");
        }

        tagRepository.delete(tag);

        // 记录操作日志
        logOperation(user.getId(), "DELETE_TAG", "TAG", tagId,
                "删除标签: " + tag.getName());
    }

    // 获取用户的所有标签
    public List<Tag> getUserTags(User user) {
        return tagRepository.findByOwnerOrderByName(user);
    }

    // 搜索标签
    public List<Tag> searchTags(String keyword, User user) {
        return tagRepository.findByNameContainingIgnoreCaseAndOwner(keyword, user);
    }

    // 获取标签详情
    public Tag getTagDetail(Long tagId, User user) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        // 检查权限（只能查看自己的标签）
        if (!tag.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("无权查看此标签");
        }

        return tag;
    }

    // 批量创建标签
    public List<Tag> batchCreateTags(List<String> tagNames, User user) {
        if (user.getRole() == Role.VIEWER) {
            throw new RuntimeException("您当前的角色为查看者，无法创建标签");
        }

        List<Tag> createdTags = new ArrayList<>();
        for (String tagName : tagNames) {
            if (tagName != null && !tagName.trim().isEmpty()) {
                String trimmedName = tagName.trim();
                // 检查是否已存在
                Optional<Tag> existingTag = tagRepository.findByNameAndOwner(trimmedName, user);
                if (existingTag.isEmpty()) {
                    Tag tag = new Tag();
                    tag.setName(trimmedName);
                    tag.setOwner(user);
                    Tag savedTag = tagRepository.save(tag);
                    createdTags.add(savedTag);
                }
            }
        }

        // 记录操作日志
        if (!createdTags.isEmpty()) {
            logOperation(user.getId(), "BATCH_CREATE_TAGS", "TAG", null,
                    "批量创建标签: " + createdTags.size() + "个");
        }

        return createdTags;
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