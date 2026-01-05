// VideoConferenceService.java
package com.example.multiuser_online_editing.service.communication;

import com.example.multiuser_online_editing.entity.communication.*;
import com.example.multiuser_online_editing.entity.document_management.Document;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.example.multiuser_online_editing.repository.communication.ConferenceMessageRepository;
import com.example.multiuser_online_editing.repository.communication.ConferenceParticipantRepository;
import com.example.multiuser_online_editing.repository.communication.VideoConferenceRepository;
import com.example.multiuser_online_editing.repository.document_management.DocumentRepository;
import com.example.multiuser_online_editing.repository.user_management.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class VideoConferenceService {

    @Autowired
    private VideoConferenceRepository conferenceRepository;

    @Autowired
    private ConferenceParticipantRepository participantRepository;

    @Autowired
    private ConferenceMessageRepository messageRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationService notificationService;

    /**
     * 创建视频会议
     */
    public VideoConference createConference(Long documentId, String title, String description,
                                            Integer maxParticipants, User creator) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        // 检查权限：只有文档所有者或协作者可以创建会议
        boolean hasPermission = document.getOwner().getId().equals(creator.getId()) ||
                document.getCollaborators().stream().anyMatch(c -> c.getId().equals(creator.getId()));

        if (!hasPermission) {
            throw new RuntimeException("无权在此文档中创建会议");
        }

        VideoConference conference = new VideoConference();
        conference.setDocument(document);
        conference.setTitle(title);
        conference.setDescription(description);
        conference.setMaxParticipants(maxParticipants != null ? maxParticipants : 10);
        conference.setCreatedBy(creator);
        conference.setStatus(ConferenceStatus.ACTIVE);

        VideoConference savedConference = conferenceRepository.save(conference);

        // 自动将创建者添加为会议主持人
        joinConference(savedConference.getConferenceId(), creator, ParticipantRole.HOST);

        // 实时同步文档的所有会议
        messagingTemplate.convertAndSend(
                "/topic/document/" + documentId + "/conferences",
                getDocumentConferences(documentId)
        );

        // 发送会议通知
        if (!Objects.equals(document.getOwner().getId(), creator.getId()))
            notificationService.sendNotification(document.getOwner().getId(), "会议创建通知",
                    creator.getUsername() + " 创建了会议，" + "文档：" + document.getTitle() +
                            "，会议名称：" + title);

        for (User collaborator: document.getCollaborators()) {
            if (!Objects.equals(collaborator.getId(), creator.getId()))
                notificationService.sendNotification(collaborator.getId(), "会议创建通知",
                        creator.getUsername() + " 创建了会议，" + "文档：" + document.getTitle() +
                                "，会议名称：" + title);
        }

        return savedConference;
    }

    /**
     * 加入视频会议
     */
    public ConferenceParticipant joinConference(String conferenceId, User user, ParticipantRole role) {
        VideoConference conference = conferenceRepository.findByConferenceId(conferenceId)
                .orElseThrow(() -> new RuntimeException("会议不存在"));

        // 检查会议状态
        if (conference.getStatus() != ConferenceStatus.ACTIVE &&
                conference.getStatus() != ConferenceStatus.IN_PROGRESS) {
            throw new RuntimeException("会议已结束或已取消");
        }

        // 检查参与者数量
        Long participantCount = participantRepository.countJoinedParticipants(conference.getId());
        if (participantCount >= conference.getMaxParticipants()) {
            throw new RuntimeException("会议人数已满");
        }

        // 检查用户是否已经在其他会议中
        boolean isInOtherConference = participantRepository.findByUserIdAndStatus(user.getId(), ParticipantStatus.JOINED)
                .stream()
                .anyMatch(participant -> !participant.getConference().getId().equals(conference.getId()));

        if (isInOtherConference) {
            throw new RuntimeException("您已经在其他会议中，请先退出其他会议再加入");
        }

        // 检查用户是否有权限加入（文档所有者或协作者）
        Document document = conference.getDocument();
        boolean hasAccess = document.getOwner().getId().equals(user.getId()) ||
                document.getCollaborators().stream().anyMatch(c -> c.getId().equals(user.getId()));

        if (!hasAccess) {
            throw new RuntimeException("无权加入此会议");
        }

        // 检查是否已经是参与者
        Optional<ConferenceParticipant> existingParticipant =
                participantRepository.findByConferenceIdAndUserId(conference.getId(), user.getId());

        ConferenceParticipant participant;
        if (existingParticipant.isPresent()) {
            participant = existingParticipant.get();
            participant.setStatus(ParticipantStatus.JOINED);
            participant.setLeftAt(null);
        } else {
            participant = new ConferenceParticipant();
            participant.setConference(conference);
            participant.setUser(user);
            participant.setRole(role != null ? role : ParticipantRole.PARTICIPANT);
            participant.setStatus(ParticipantStatus.JOINED);
        }

        ConferenceParticipant savedParticipant = participantRepository.save(participant);

        // 如果会议状态是ACTIVE，更新为IN_PROGRESS
        if (conference.getStatus() == ConferenceStatus.ACTIVE) {
            conference.setStatus(ConferenceStatus.IN_PROGRESS);
            conference.setStartedAt(LocalDateTime.now());
            conferenceRepository.save(conference);
        }

        // 实时同步会议的所有参与者
        broadcastParticipantUpdate(conference);

        // 发送有人加入会议的系统消息
        sendSystemMessage(conference, user.getUsername() + " 加入了会议");

        return savedParticipant;
    }

    /**
     * 离开视频会议
     */
    public void leaveConference(String conferenceId, Long userId) {
        VideoConference conference = conferenceRepository.findByConferenceId(conferenceId)
                .orElseThrow(() -> new RuntimeException("会议不存在"));

        ConferenceParticipant participant = participantRepository
                .findByConferenceIdAndUserId(conference.getId(), userId)
                .orElseThrow(() -> new RuntimeException("参与者不存在"));

        participant.setStatus(ParticipantStatus.LEFT);
        participant.setIsAudioEnabled(true);
        participant.setIsVideoEnabled(true);
        participant.setIsSharingScreen(false);
        participant.setLeftAt(LocalDateTime.now());
        participantRepository.save(participant);

        // 发送有人离开会议的系统消息
        sendSystemMessage(conference, participant.getUser().getUsername() + " 离开了会议");

        // 实时同步会议的所有参与者
        broadcastParticipantUpdate(conference);

//        // 检查是否还有参与者
//        Long activeParticipants = participantRepository.countJoinedParticipants(conference.getId());
//        if (activeParticipants == 0) {
//            // 如果没有参与者，结束会议
//            endConference(conferenceId, userId);
//        } else {
//            // 实时同步会议的所有参与者
//            broadcastParticipantUpdate(conference);
//        }
    }

    /**
     * 结束视频会议
     */
    public void endConference(String conferenceId, Long userId) {
        VideoConference conference = conferenceRepository.findByConferenceId(conferenceId)
                .orElseThrow(() -> new RuntimeException("会议不存在"));

        // 检查权限：只有主持人可以结束会议
        ConferenceParticipant participant = participantRepository
                .findByConferenceIdAndUserId(conference.getId(), userId)
                .orElseThrow(() -> new RuntimeException("参与者不存在"));

        if (participant.getRole() != ParticipantRole.HOST) {
            throw new RuntimeException("只有主持人可以结束会议");
        }

        conference.setStatus(ConferenceStatus.ENDED);
        conference.setEndedAt(LocalDateTime.now());
        conferenceRepository.save(conference);

        // 更新所有参与者的状态
        for (ConferenceParticipant conferenceParticipant: conference.getParticipants()) {
            participantRepository.updateParticipantStatus(conference.getId(),
                    conferenceParticipant.getUser().getId(), ParticipantStatus.LEFT);
        }

        // 发送会议结束的系统消息
        sendSystemMessage(conference, "会议已结束");

        // 广播会议结束通知
        messagingTemplate.convertAndSend(
                "/topic/conference/" + conferenceId + "/ended",
                new ConferenceEndedMessage(conferenceId, "会议已结束")
        );

        // 实时同步文档的所有会议
        messagingTemplate.convertAndSend(
                "/topic/document/" + conference.getDocument().getId() + "/conferences",
                getDocumentConferences(conference.getDocument().getId())
        );
    }

    /**
     * 发送聊天消息
     */
    public ConferenceMessage sendMessage(String conferenceId, Long userId, String content) {
        VideoConference conference = conferenceRepository.findByConferenceId(conferenceId)
                .orElseThrow(() -> new RuntimeException("会议不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查用户是否是会议参与者
        boolean isParticipant = participantRepository.existsByConferenceIdAndUserIdAndStatus(
                conference.getId(), userId, ParticipantStatus.JOINED);

        if (!isParticipant) {
            throw new RuntimeException("只有会议参与者可以发送消息");
        }

        ConferenceMessage message = new ConferenceMessage();
        message.setConference(conference);
        message.setUser(user);
        message.setContent(content);

        ConferenceMessage savedMessage = messageRepository.save(message);

        // 实时同步聊天消息
//        messagingTemplate.convertAndSend(
//                "/topic/conference/" + conferenceId + "/messages",
//                new ChatMessageDTO(savedMessage)
//        );

        return savedMessage;
    }


    /**
     * 获取会议消息历史
     */
    public List<ConferenceMessage> getMessageHistory(String conferenceId) {
        VideoConference conference = conferenceRepository.findByConferenceId(conferenceId)
                .orElseThrow(() -> new RuntimeException("会议不存在"));

        return messageRepository.findByConferenceIdOrderBySentAtAsc(conferenceId);
    }

    /**
     * 获取会议参与者列表
     */
    public List<ConferenceParticipant> getConferenceParticipants(String conferenceId) {
        VideoConference conference = conferenceRepository.findByConferenceId(conferenceId)
                .orElseThrow(() -> new RuntimeException("会议不存在"));

        return participantRepository.findByConferenceIdAndStatus(conference.getId(), ParticipantStatus.JOINED);
    }

    /**
     * 获取文档的所有会议
     */
    public List<VideoConference> getDocumentConferences(Long documentId) {
        return conferenceRepository.findByDocumentIdAndStatusNot(documentId, ConferenceStatus.ENDED);
    }

    /**
     * 切换屏幕共享状态
     */
    public void toggleScreenSharing(String conferenceId, Long userId, Boolean isSharing) {
        VideoConference conference = conferenceRepository.findByConferenceId(conferenceId)
                .orElseThrow(() -> new RuntimeException("会议不存在"));

        ConferenceParticipant participant = participantRepository
                .findByConferenceIdAndUserId(conference.getId(), userId)
                .orElseThrow(() -> new RuntimeException("参与者不存在"));

        // 如果要开启屏幕共享，检查是否已经有其他人在共享
        if (isSharing) {
            List<ConferenceParticipant> allParticipants = participantRepository
                    .findByConferenceIdAndStatus(conference.getId(), ParticipantStatus.JOINED);

            boolean someoneElseSharing = allParticipants.stream()
                    .filter(p -> !p.getUser().getId().equals(userId)) // 排除自己
                    .anyMatch(ConferenceParticipant::getIsSharingScreen);

            if (someoneElseSharing) {
                throw new RuntimeException("已有其他参与者在共享屏幕，请等待其结束共享");
            }
        }


        participant.setIsSharingScreen(isSharing);
        participantRepository.save(participant);

        // 实时同步屏幕共享状态
        messagingTemplate.convertAndSend(
                "/topic/conference/" + conferenceId + "/screen-sharing",
                new ScreenSharingDTO(userId, isSharing)
        );

        // 发送系统消息通知
        if (isSharing) {
            sendSystemMessage(conference, participant.getUser().getUsername() + " 开始共享屏幕");
        } else {
            sendSystemMessage(conference, participant.getUser().getUsername() + " 停止共享屏幕");
        }
    }

    /**
     * 切换音视频状态
     */
    public void toggleMedia(String conferenceId, Long userId, Boolean videoEnabled, Boolean audioEnabled) {
        VideoConference conference = conferenceRepository.findByConferenceId(conferenceId)
                .orElseThrow(() -> new RuntimeException("会议不存在"));

        ConferenceParticipant participant = participantRepository
                .findByConferenceIdAndUserId(conference.getId(), userId)
                .orElseThrow(() -> new RuntimeException("参与者不存在"));

        if (videoEnabled != null) {
            participant.setIsVideoEnabled(videoEnabled);
        }
        if (audioEnabled != null) {
            participant.setIsAudioEnabled(audioEnabled);
        }

        participantRepository.save(participant);

        // 实时同步用户的音视频状态
        messagingTemplate.convertAndSend(
                "/topic/conference/" + conferenceId + "/media-status",
                new MediaStatusDTO(userId,
                        participant.getIsVideoEnabled(),
                        participant.getIsAudioEnabled())
        );
    }

    /**
     * 发送系统消息
     */
    private void sendSystemMessage(VideoConference conference, String content) {
        ConferenceMessage systemMessage = new ConferenceMessage();
        systemMessage.setConference(conference);
        systemMessage.setContent(content);
        // 注意：系统消息没有关联用户
        messageRepository.save(systemMessage);

        // 实时同步系统消息
        messagingTemplate.convertAndSend(
                "/topic/conference/" + conference.getConferenceId() + "/messages",
                new ChatMessageDTO(systemMessage)
        );
    }

    /**
     * 广播参与者更新
     */
    private void broadcastParticipantUpdate(VideoConference conference) {
        List<ConferenceParticipant> participants = getConferenceParticipants(conference.getConferenceId());

        // 实时同步参与者更新
        messagingTemplate.convertAndSend(
                "/topic/conference/" + conference.getConferenceId() + "/participants",
                participants.stream().map(ParticipantDTO::new).toList()
        );
    }

    public ConferenceParticipant getParticipant(String conferenceId, Long userId) {
        VideoConference conference = conferenceRepository.findByConferenceId(conferenceId)
                .orElseThrow(() -> new RuntimeException("会议不存在"));

        return participantRepository.findByConferenceIdAndUserId(conference.getId(), userId)
                .orElse(null);
    }

    // DTO类
    public static class ChatMessageDTO {
        public Long id;
        public Long userId;
        public String username;
        public String content;
        public LocalDateTime sentAt;

        public ChatMessageDTO(ConferenceMessage message) {
            this.id = message.getId();
            this.userId = message.getUser() != null ? message.getUser().getId() : null;
            this.username = message.getUser() != null ? message.getUser().getUsername() : "系统";
            this.content = message.getContent();
            this.sentAt = message.getSentAt();
        }
    }

    /*
    public static class ConferenceDTO {
        private Long id;
        private String conferenceId;
        private String title;
        private String description;
        private String createdUsername;
        private ConferenceStatus status;
        private Integer maxParticipants;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
        private Integer participantsNumber;

        public ConferenceDTO(VideoConference conference) {
            this.id = conference.getId();
            this.conferenceId = conference.getConferenceId();
            this.title = conference.getTitle();
            this.description = conference.getDescription();
            this.createdUsername = conference.getCreatedBy().getUsername();
            this.status = conference.getStatus();
            this.maxParticipants = conference.getMaxParticipants();
            this.createdAt = conference.getCreatedAt();
            this.updatedAt = conference.getUpdatedAt();
            this.startedAt = conference.getStartedAt();
            this.endedAt = conference.getEndedAt();
            this.participantsNumber = conference.getParticipants().size();
        }
    }
    */

    public static class ParticipantDTO {
        public Long userId;
        public String username;
        public String avatarPath;
        public ParticipantRole role;
        public Boolean isSharingScreen;
        public Boolean isVideoEnabled;
        public Boolean isAudioEnabled;

        public ParticipantDTO(ConferenceParticipant participant) {
            this.userId = participant.getUser().getId();
            this.username = participant.getUser().getUsername();
            this.avatarPath = participant.getUser().getAvatarPath();
            this.role = participant.getRole();
            this.isSharingScreen = participant.getIsSharingScreen();
            this.isVideoEnabled = participant.getIsVideoEnabled();
            this.isAudioEnabled = participant.getIsAudioEnabled();
        }
    }

    public static class ScreenSharingDTO {
        public Long userId;
        public Boolean isSharing;

        public ScreenSharingDTO(Long userId, Boolean isSharing) {
            this.userId = userId;
            this.isSharing = isSharing;
        }
    }

    public static class MediaStatusDTO {
        public Long userId;
        public Boolean videoEnabled;
        public Boolean audioEnabled;

        public MediaStatusDTO(Long userId, Boolean videoEnabled, Boolean audioEnabled) {
            this.userId = userId;
            this.videoEnabled = videoEnabled;
            this.audioEnabled = audioEnabled;
        }
    }

    public static class ConferenceEndedMessage {
        public String conferenceId;
        public String message;

        public ConferenceEndedMessage(String conferenceId, String message) {
            this.conferenceId = conferenceId;
            this.message = message;
        }
    }

    public static class VideoFrameDTO {
        public Long userId;
        public String frameData;
        public Long timestamp;
        public Integer width;
        public Integer height;

        public VideoFrameDTO(Long userId, String frameData, Long timestamp, Integer width, Integer height) {
            this.userId = userId;
            this.frameData = frameData;
            this.timestamp = timestamp;
            this.width = width;
            this.height = height;
        }

        public Long getUserId() {
            return userId;
        }
    }

    public static class AudioDataDTO {
        public Long userId;
        public String audioData;
        public Integer sampleRate;
        public Integer channels;

        public AudioDataDTO(Long userId, String audioData, Integer sampleRate, Integer channels) {
            this.userId = userId;
            this.audioData = audioData;
            this.sampleRate = sampleRate;
            this.channels = channels;
        }

        public Long getUserId() {
            return userId;
        }
    }
}