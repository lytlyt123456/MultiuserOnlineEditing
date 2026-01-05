package com.example.multiuser_online_editing.service.collaboration;

import com.example.multiuser_online_editing.entity.collaboration.CollaborationSession;
import com.example.multiuser_online_editing.entity.document_management.Document;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.example.multiuser_online_editing.repository.collaboration.CollaborationSessionRepository;
import com.example.multiuser_online_editing.repository.document_management.DocumentRepository;
import com.example.multiuser_online_editing.repository.user_management.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CollaborationService {

    @Autowired
    private CollaborationSessionRepository collaborationSessionRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 用户加入文档协作
     */
    public void joinDocument(Long documentId, Long userId, String sessionId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查权限：用户必须是文档所有者或协作者
        boolean hasPermission = document.getOwner().getId().equals(userId) ||
                document.getCollaborators().stream().anyMatch(c -> c.getId().equals(userId));

        if (!hasPermission) {
            throw new RuntimeException("无权访问此文档");
        }

        // 创建或更新协作会话
        Optional<CollaborationSession> existingSession =
                collaborationSessionRepository.findByUserIdAndDocumentId(userId, documentId);

        CollaborationSession session;
        if (existingSession.isPresent()) {
            session = existingSession.get();
            session.setSessionId(sessionId);
            session.setLastActivity(LocalDateTime.now());
        } else {
            session = new CollaborationSession(document, user, sessionId);
        }

        collaborationSessionRepository.save(session);

        // 广播用户加入通知
        messagingTemplate.convertAndSend(
                "/topic/document/" + documentId + "/users",
                getOnlineUsers(documentId)
        );
    }

    /**
     * 用户离开文档协作
     */
    public void leaveDocument(Long documentId, Long userId) {
        Optional<CollaborationSession> session =
                collaborationSessionRepository.findByUserIdAndDocumentId(userId, documentId);

        if (session.isPresent()) {
            collaborationSessionRepository.delete(session.get());

            // 广播用户离开通知
            messagingTemplate.convertAndSend(
                    "/topic/document/" + documentId + "/users",
                    getOnlineUsers(documentId)
            );
        }
    }

    /**
     * 检查文档是否存在
     */
    public void checkDocumentExistence(Long documentId) {
        // 更新文档内容
        documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));
    }

    /**
     * 获取文档的在线用户
     */
    public List<User> getOnlineUsers(Long documentId) {
        List<CollaborationSession> sessions = collaborationSessionRepository.findByDocumentId(documentId);
        return sessions.stream()
                .map(CollaborationSession::getUser)
                .collect(Collectors.toList());
    }
}