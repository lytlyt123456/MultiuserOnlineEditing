package com.example.multiuser_online_editing.service.collaboration;

import com.example.multiuser_online_editing.entity.collaboration.Comment;
import com.example.multiuser_online_editing.entity.document_management.Document;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.example.multiuser_online_editing.repository.collaboration.CommentRepository;
import com.example.multiuser_online_editing.repository.document_management.DocumentRepository;
import com.example.multiuser_online_editing.repository.user_management.UserRepository;
import com.example.multiuser_online_editing.service.communication.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 添加评论
     */
    public Comment addComment(Long documentId, Long userId, String content, Integer position) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查权限
        boolean hasPermission = document.getOwner().getId().equals(userId) ||
                document.getCollaborators().stream().anyMatch(c -> c.getId().equals(userId));

        if (!hasPermission) {
            throw new RuntimeException("无权评论此文档");
        }

        Comment comment = new Comment(document, user, content, position);

        // 处理@提及
        processMentions(comment, content);

        Comment savedComment = commentRepository.save(comment);

        // 广播新评论通知
        messagingTemplate.convertAndSend(
                "/topic/document/" + documentId + "/comments",
                getDocumentComments(documentId)
        );

        return savedComment;
    }

    /**
     * 回复评论
     */
    public Comment replyToComment(Long commentId, Long userId, String content) {
        Comment parent = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查权限
        Document document = parent.getDocument();
        boolean hasPermission = document.getOwner().getId().equals(userId) ||
                document.getCollaborators().stream().anyMatch(c -> c.getId().equals(userId));

        if (!hasPermission) {
            throw new RuntimeException("无权回复此评论");
        }

        Comment reply = new Comment(document, user, content, parent.getPosition());
        reply.setParent(parent);

        // 处理@提及
        processMentions(reply, content);

        Comment savedReply = commentRepository.save(reply);

        // 通知父评论的作者
        if (!parent.getUser().getId().equals(userId)) {
            notificationService.sendNotification(
                    parent.getUser().getId(),
                    "新回复",
                    user.getUsername() + " 回复了您的评论"
            );
        }

        // 广播新回复通知
        messagingTemplate.convertAndSend(
                "/topic/document/" + document.getId() + "/comments",
                getDocumentComments(document.getId())
        );

        return savedReply;
    }

    /**
     * 标记评论为已解决
     */
    public void resolveComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        // 检查权限：只有文档所有者或评论作者可以标记解决
        boolean canResolve = comment.getDocument().getOwner().getId().equals(userId) ||
                comment.getUser().getId().equals(userId);

        if (!canResolve) {
            throw new RuntimeException("无权标记此评论为已解决");
        }

        comment.setResolved(true);
        commentRepository.save(comment);

        // 广播评论更新
        messagingTemplate.convertAndSend(
                "/topic/document/" + comment.getDocument().getId() + "/comments",
                getDocumentComments(comment.getDocument().getId())
        );
    }

    /**
     * 删除评论
     */
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        // 检查权限：只有文档所有者或评论作者可以删除
        boolean canDelete = comment.getDocument().getOwner().getId().equals(userId) ||
                comment.getUser().getId().equals(userId);

        if (!canDelete) {
            throw new RuntimeException("无权删除此评论");
        }

        commentRepository.delete(comment);

        // 广播评论删除
        messagingTemplate.convertAndSend(
                "/topic/document/" + comment.getDocument().getId() + "/comments",
                getDocumentComments(comment.getDocument().getId())
        );
    }

    /**
     * 获取文档的所有评论
     */
    public List<Comment> getDocumentComments(Long documentId) {
        return commentRepository.findByDocumentIdOrderByCreatedAtAsc(documentId);
    }

    /**
     * 处理@提及
     */
    private void processMentions(Comment comment, String content) {
        // 使用正则表达式匹配@用户名
        Pattern pattern = Pattern.compile("@(\\w+)");
        // @ 匹配字面量的@符号
        // (\\w+) 捕获组，匹配一个或多个单词字符（字母、数字、下划线）

        Matcher matcher = pattern.matcher(content);
        // 创建一个匹配器对象，用于在指定的内容字符串中查找符合模式的匹配项

        while (matcher.find()) {
            String username = matcher.group(1); // 获取第一个捕获组的内容（即(\\w+)匹配的部分）
            Optional<User> mentionedUser = userRepository.findByUsername(username);

            if (mentionedUser.isPresent()) {
                User user = mentionedUser.get();
                // 检查被提及的用户是否有权限访问文档
                boolean hasAccess = comment.getDocument().getOwner().getId().equals(user.getId()) ||
                        comment.getDocument().getCollaborators().stream()
                                .anyMatch(c -> c.getId().equals(user.getId()));

                if (hasAccess && !comment.getUser().getId().equals(user.getId())) {
                    comment.getMentionedUsers().add(user);

                    // 发送提及通知
                    notificationService.sendNotification(
                            user.getId(),
                            "您被提及了",
                            comment.getUser().getUsername() + " 在评论中提到了您"
                    );
                }
            }
        }
    }
}