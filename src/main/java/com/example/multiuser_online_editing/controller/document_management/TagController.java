package com.example.multiuser_online_editing.controller.document_management;

import com.example.multiuser_online_editing.controller.ApiResponse;
import com.example.multiuser_online_editing.entity.document_management.Tag;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.example.multiuser_online_editing.service.document_management.TagService;
import com.example.multiuser_online_editing.service.user_management.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    @Autowired
    private TagService tagService;

    @Autowired
    private UserService userService;

    public static Map<String, Object> tagToMap(Tag tag) {
        Map<String, Object> tag_res = new HashMap<>();
        tag_res.put("id", tag.getId());
        tag_res.put("name", tag.getName());
        tag_res.put("description", tag.getDescription());
        tag_res.put("owner_id", tag.getOwner().getId());
        tag_res.put("created_at", tag.getCreatedAt());

        return tag_res;
    }

    public static List<Map<String, Object>> tagsToMaps(List<Tag> tags) {
        List<Map<String, Object>> tags_res = new ArrayList<>();
        for (Tag tag : tags)
            tags_res.add(tagToMap(tag));
        return tags_res;
    }

    // 创建标签
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createTag(@RequestBody CreateTagRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Tag tag = tagService.createTag(
                    request.getName(),
                    request.getDescription(),
                    currentUser
            );

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("tagId", tag.getId());
            responseData.put("name", tag.getName());

            return ResponseEntity.ok(ApiResponse.success("标签创建成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 更新标签
    @PutMapping("/{tagId}")
    public ResponseEntity<ApiResponse<Object>> updateTag(
            @PathVariable Long tagId,
            @RequestBody UpdateTagRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Tag tag = tagService.updateTag(
                    tagId,
                    request.getName(),
                    request.getDescription(),
                    currentUser
            );

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("tagId", tag.getId());
            responseData.put("name", tag.getName());

            return ResponseEntity.ok(ApiResponse.success("标签更新成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 删除标签
    @DeleteMapping("/{tagId}")
    public ResponseEntity<ApiResponse<Object>> deleteTag(@PathVariable Long tagId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            tagService.deleteTag(tagId, currentUser);

            return ResponseEntity.ok(ApiResponse.success("标签删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 获取用户的所有标签
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getUserTags() {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            List<Tag> tags = tagService.getUserTags(currentUser);

            List<Map<String, Object>> tags_res = tagsToMaps(tags);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("tags", tags_res);

            return ResponseEntity.ok(ApiResponse.success("获取标签列表成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 搜索标签
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Object>> searchTags(@RequestParam String keyword) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            List<Tag> tags = tagService.searchTags(keyword, currentUser);

            List<Map<String, Object>> tags_res = tagsToMaps(tags);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("tags", tags_res);

            return ResponseEntity.ok(ApiResponse.success("搜索标签成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 获取标签详情
    @GetMapping("/{tagId}")
    public ResponseEntity<ApiResponse<Object>> getTagDetail(@PathVariable Long tagId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Tag tag = tagService.getTagDetail(tagId, currentUser);

            Map<String, Object> responseData = tagToMap(tag);

            return ResponseEntity.ok(ApiResponse.success("获取标签详情成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 批量创建标签
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<Object>> batchCreateTags(@RequestBody BatchCreateTagsRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            List<Tag> createdTags = tagService.batchCreateTags(request.getTagNames(), currentUser);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("createdCount", createdTags.size());

            return ResponseEntity.ok(ApiResponse.success("批量创建标签成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

// 请求DTO类
class CreateTagRequest {
    private String name;
    private String description;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

class UpdateTagRequest {
    private String name;
    private String description;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

class BatchCreateTagsRequest {
    private List<String> tagNames;

    // Getters and Setters
    public List<String> getTagNames() { return tagNames; }
    public void setTagNames(List<String> tagNames) { this.tagNames = tagNames; }
}