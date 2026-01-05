package com.example.multiuser_online_editing.controller.collaboration;

import com.example.multiuser_online_editing.controller.ApiResponse;
import com.example.multiuser_online_editing.entity.collaboration.Comment;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.example.multiuser_online_editing.service.collaboration.CommentService;
import com.example.multiuser_online_editing.service.user_management.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    /**
     * 添加评论
     */
    @PostMapping("/{documentId}")
    public ResponseEntity<ApiResponse<Object>> addComment(
            @PathVariable Long documentId,
            @RequestBody AddCommentRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Comment comment = commentService.addComment(
                    documentId,
                    currentUserId,
                    request.getContent(),
                    request.getPosition()
            );

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("commentId", comment.getId());
            responseData.put("content", comment.getContent());
            responseData.put("position", comment.getPosition());

            return ResponseEntity.ok(ApiResponse.success("评论添加成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 回复评论
     */
    @PostMapping("/{commentId}/reply")
    public ResponseEntity<ApiResponse<Object>> replyToComment(
            @PathVariable Long commentId,
            @RequestBody ReplyCommentRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            Comment reply = commentService.replyToComment(commentId, currentUserId, request.getContent());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("commentId", reply.getId());
            responseData.put("content", reply.getContent());
            responseData.put("parentId", reply.getParent().getId());

            return ResponseEntity.ok(ApiResponse.success("回复添加成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 标记评论为已解决
     */
    @PutMapping("/{commentId}/resolve")
    public ResponseEntity<ApiResponse<Object>> resolveComment(@PathVariable Long commentId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            commentService.resolveComment(commentId, currentUserId);

            return ResponseEntity.ok(ApiResponse.success("评论已标记为已解决"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Object>> deleteComment(@PathVariable Long commentId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            commentService.deleteComment(commentId, currentUserId);

            return ResponseEntity.ok(ApiResponse.success("评论删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取文档的所有评论
     */
    @GetMapping("/document/{documentId}")
    public ResponseEntity<ApiResponse<Object>> getDocumentComments(@PathVariable Long documentId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            List<Comment> comments = commentService.getDocumentComments(documentId);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("comments", commentsToMaps(comments));

            return ResponseEntity.ok(ApiResponse.success("获取评论成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 工具方法：评论列表转换为Map
    private List<Map<String, Object>> commentsToMaps(List<Comment> comments) {
        return comments.stream()
                .map(this::commentToMap)
                .toList();
    }

    private Map<String, Object> commentToMap(Comment comment) {
        Map<String, Object> commentMap = new HashMap<>();
        commentMap.put("id", comment.getId());
        commentMap.put("content", comment.getContent());
        commentMap.put("position", comment.getPosition());
        commentMap.put("resolved", comment.getResolved());
        commentMap.put("createdAt", comment.getCreatedAt());
        commentMap.put("updatedAt", comment.getUpdatedAt());

        // 用户信息
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", comment.getUser().getId());
        userMap.put("username", comment.getUser().getUsername());
        commentMap.put("user", userMap);

        // 父评论信息
        if (comment.getParent() != null) {
            Map<String, Object> parentMap = new HashMap<>();
            parentMap.put("id", comment.getParent().getId());
            parentMap.put("content", comment.getParent().getContent());
            commentMap.put("parent", parentMap);
        }

        // 回复列表
        if (!comment.getReplies().isEmpty()) {
            commentMap.put("replies", commentsToMaps(comment.getReplies()));
        }

        // 被提及的用户
        if (!comment.getMentionedUsers().isEmpty()) {
            List<Map<String, Object>> mentionedUsers = comment.getMentionedUsers().stream()
                    .map(user -> {
                        Map<String, Object> mentionedUser = new HashMap<>();
                        mentionedUser.put("id", user.getId());
                        mentionedUser.put("username", user.getUsername());
                        return mentionedUser;
                    })
                    .toList();
            commentMap.put("mentionedUsers", mentionedUsers);
        }

        return commentMap;
    }

    // 请求DTO类
    static class AddCommentRequest {
        private String content;
        private Integer position;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Integer getPosition() { return position; }
        public void setPosition(Integer position) { this.position = position; }
    }

    static class ReplyCommentRequest {
        private String content;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}