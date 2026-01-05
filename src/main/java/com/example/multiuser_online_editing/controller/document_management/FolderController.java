package com.example.multiuser_online_editing.controller.document_management;

import com.example.multiuser_online_editing.controller.ApiResponse;
import com.example.multiuser_online_editing.entity.document_management.Document;
import com.example.multiuser_online_editing.entity.document_management.Folder;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.example.multiuser_online_editing.service.document_management.FolderService;
import com.example.multiuser_online_editing.service.user_management.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

    @Autowired
    private FolderService folderService;

    @Autowired
    private UserService userService;

    public static Map<String, Object> folderToMap(Folder folder) {
        Map<String, Object> folder_res = new HashMap<>();
        folder_res.put("id", folder.getId());
        folder_res.put("name", folder.getName());
        folder_res.put("description", folder.getDescription());
        if (folder.getParent() != null) {
            folder_res.put("parent_id", folder.getParent().getId());
            folder_res.put("parent_name", folder.getParent().getName());
        } else {
            folder_res.put("parent_id", null);
            folder_res.put("parent_name", null);
        }
        folder_res.put("created_at", folder.getCreatedAt());
        folder_res.put("updated_at", folder.getUpdatedAt());

        return folder_res;
    }

    public static List<Map<String, Object>> foldersToMaps(List<Folder> folders) {
        List<Map<String, Object>> folders_res = new ArrayList<>();
        for (Folder folder: folders)
            folders_res.add(folderToMap(folder));

        return folders_res;
    }

    // 创建文件夹
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createFolder(@RequestBody CreateFolderRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Folder folder = folderService.createFolder(
                    request.getName(),
                    request.getDescription(),
                    request.getParentId(),
                    currentUser
            );

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("folderId", folder.getId());
            responseData.put("name", folder.getName());

            return ResponseEntity.ok(ApiResponse.success("文件夹创建成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 更新文件夹
    @PutMapping("/{folderId}")
    public ResponseEntity<ApiResponse<Object>> updateFolder(
            @PathVariable Long folderId,
            @RequestBody UpdateFolderRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Folder folder = folderService.updateFolder(
                    folderId,
                    request.getName(),
                    request.getDescription(),
                    currentUser
            );

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("folderId", folder.getId());
            responseData.put("name", folder.getName());

            return ResponseEntity.ok(ApiResponse.success("文件夹更新成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 删除文件夹
    @DeleteMapping("/{folderId}")
    public ResponseEntity<ApiResponse<Object>> deleteFolder(@PathVariable Long folderId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            folderService.deleteFolder(folderId, currentUser);

            return ResponseEntity.ok(ApiResponse.success("文件夹删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 获取根文件夹列表
    @GetMapping("/root")
    public ResponseEntity<ApiResponse<Object>> getRootFolders() {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            List<Folder> folders = folderService.getRootFolders(currentUser);

            List<Map<String, Object>> folders_res = foldersToMaps(folders);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("folders", folders_res);

            return ResponseEntity.ok(ApiResponse.success("获取根文件夹列表成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 获取子文件夹列表
    @GetMapping("/{folderId}/subfolders")
    public ResponseEntity<ApiResponse<Object>> getSubFolders(@PathVariable Long folderId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            List<Folder> subFolders = folderService.getSubFolders(folderId, currentUser);

            List<Map<String, Object>> subFolders_res = foldersToMaps(subFolders);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("subFolders", subFolders_res);

            return ResponseEntity.ok(ApiResponse.success("获取子文件夹列表成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 获取子文件列表
    @GetMapping("/{folderId}subdocuments")
    public ResponseEntity<ApiResponse<Object>> getSubDocuments(@PathVariable Long folderId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            List<Document> subDocuments = folderService.getSubDocuments(folderId, currentUser);

            List<Map<String, Object>> subDocuments_res = DocumentController.documentsToMaps(subDocuments);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("subDocuments", subDocuments_res);

            return ResponseEntity.ok(ApiResponse.success("获取子文件夹列表成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 获取文件夹详情
    @GetMapping("/{folderId}")
    public ResponseEntity<ApiResponse<Object>> getFolderDetail(@PathVariable Long folderId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Folder folder = folderService.getFolderDetail(folderId, currentUser);

            Map<String, Object> folder_res = folderToMap(folder);

            return ResponseEntity.ok(ApiResponse.success("获取文件夹详情成功", folder_res));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 移动文件夹
    @PutMapping("/{folderId}/move")
    public ResponseEntity<ApiResponse<Object>> moveFolder(
            @PathVariable Long folderId,
            @RequestBody MoveFolderRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Folder folder = folderService.moveFolder(folderId, request.getNewParentId(), currentUser);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("folderId", folder.getId());
            responseData.put("name", folder.getName());
            responseData.put("parentId", folder.getParent() != null ? folder.getParent().getId() : null);

            return ResponseEntity.ok(ApiResponse.success("文件夹移动成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

// 请求DTO类
class CreateFolderRequest {
    private String name;
    private String description;
    private Long parentId;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
}

class UpdateFolderRequest {
    private String name;
    private String description;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

class MoveFolderRequest {
    private Long newParentId;

    // Getters and Setters
    public Long getNewParentId() { return newParentId; }
    public void setNewParentId(Long newParentId) { this.newParentId = newParentId; }
}