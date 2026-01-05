package com.example.multiuser_online_editing.controller.collaboration;

import com.example.multiuser_online_editing.controller.ApiResponse;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.example.multiuser_online_editing.service.collaboration.CollaborationService;
import com.example.multiuser_online_editing.service.user_management.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collaboration")
public class CollaborationController {

    @Autowired
    private CollaborationService collaborationService;

    @Autowired
    private UserService userService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 加入文档协作
     */
    @PostMapping("/{documentId}/join")
    public ResponseEntity<ApiResponse<Object>> joinDocument(
            @PathVariable Long documentId,
            @RequestBody JoinDocumentRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            collaborationService.joinDocument(documentId, currentUserId, request.getSessionId());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("documentId", documentId);
            responseData.put("userId", currentUserId);

            return ResponseEntity.ok(ApiResponse.success("加入文档协作成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 离开文档协作
     */
    @PostMapping("/{documentId}/leave")
    public ResponseEntity<ApiResponse<Object>> leaveDocument(@PathVariable Long documentId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            collaborationService.leaveDocument(documentId, currentUserId);

            return ResponseEntity.ok(ApiResponse.success("离开文档协作成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取文档在线用户
     */
    @GetMapping("/{documentId}/online-users")
    public ResponseEntity<ApiResponse<Object>> getOnlineUsers(@PathVariable Long documentId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            List<User> onlineUsers = collaborationService.getOnlineUsers(documentId);

            List<Map<String, Object>> onlineUsers_res = new ArrayList<>();
            for (User user: onlineUsers) {
                Map<String, Object> user_res = new HashMap<>();
                user_res.put("id", user.getId());
                user_res.put("avatar_path", user.getAvatarPath());
                user_res.put("username", user.getUsername());
                user_res.put("email", user.getEmail());
                user_res.put("phone", user.getPhone());
                user_res.put("full_name", user.getFullName());
                user_res.put("role", user.getRole());
                user_res.put("biography", user.getBio());
                onlineUsers_res.add(user_res);
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("onlineUsers", onlineUsers_res);
            responseData.put("count", onlineUsers.size());

            System.out.println("number of online users: " + ((ArrayList)(responseData.get("onlineUsers"))).size());

            return ResponseEntity.ok(ApiResponse.success("获取在线用户成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // WebSocket消息处理

    /**
     * 处理实时内容更新
     */
    @MessageMapping("/document/{documentId}/content")
    @SendTo("/topic/document/{documentId}/content")
    public ContentUpdateMessage handleContentUpdate(
            @DestinationVariable Long documentId,
            ContentUpdateMessage message) {
        try {
            collaborationService.checkDocumentExistence(documentId);
            return message;
        } catch (Exception e) {
            // 处理错误
            return new ContentUpdateMessage("", 0L);
        }
    }

    /**
     * 处理光标位置更新
     */
    @MessageMapping("/document/{documentId}/cursor")
    @SendTo("/topic/document/{documentId}/cursors")
    public CursorUpdateMessage handleCursorUpdate(
            @DestinationVariable Long documentId,
            CursorUpdateMessage message) {
        try {
            collaborationService.checkDocumentExistence(documentId);
            return message;
        } catch (Exception e) {
            // 处理错误
            return new CursorUpdateMessage(0, 0L);
        }
    }

    // 请求DTO类
    static class JoinDocumentRequest {
        private String sessionId;

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }

    // WebSocket消息类
    static class ContentUpdateMessage {
        private String content;
        private Long userId;

        public ContentUpdateMessage() {}

        public ContentUpdateMessage(String content, Long userId) {
            this.content = content;
            this.userId = userId;
        }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    static class CursorUpdateMessage {
        private Integer position;
        private Long userId;

        public CursorUpdateMessage() {}

        public CursorUpdateMessage(Integer position, Long userId) {
            this.position = position;
            this.userId = userId;
        }

        public Integer getPosition() { return position; }
        public void setPosition(Integer position) { this.position = position; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }
}