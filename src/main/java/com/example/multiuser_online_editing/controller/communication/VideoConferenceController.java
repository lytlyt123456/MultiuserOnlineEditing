package com.example.multiuser_online_editing.controller.communication;

import com.example.multiuser_online_editing.controller.ApiResponse;
import com.example.multiuser_online_editing.service.communication.VideoConferenceService.*;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.example.multiuser_online_editing.entity.communication.*;
import com.example.multiuser_online_editing.service.communication.VideoConferenceService;
import com.example.multiuser_online_editing.service.user_management.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/video-conference")
public class VideoConferenceController {

    @Autowired
    private VideoConferenceService videoConferenceService;

    @Autowired
    private UserService userService;

    /**
     * 创建视频会议
     */
    @PostMapping("/document/{documentId}")
    public ResponseEntity<ApiResponse<Object>> createConference(
            @PathVariable Long documentId,
            @RequestBody CreateConferenceRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            var conference = videoConferenceService.createConference(
                    documentId,
                    request.getTitle(),
                    request.getDescription(),
                    request.getMaxParticipants(),
                    currentUser
            );

            Map<String, Object> responseData = Map.of(
                    "conferenceId", conference.getConferenceId(),
                    "title", conference.getTitle()
            );

            return ResponseEntity.ok(ApiResponse.success("视频会议创建成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 加入视频会议
     */
    @PostMapping("/{conferenceId}/join")
    public ResponseEntity<ApiResponse<Object>> joinConference(@PathVariable String conferenceId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            User currentUser = userService.getUserProfile(currentUserId);

            var participant = videoConferenceService.joinConference(conferenceId, currentUser, ParticipantRole.PARTICIPANT);

            Map<String, Object> responseData = Map.of(
                    "conferenceId", conferenceId,
                    "role", participant.getRole()
            );

            return ResponseEntity.ok(ApiResponse.success("加入会议成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 离开视频会议
     */
    @PostMapping("/{conferenceId}/leave")
    public ResponseEntity<ApiResponse<Object>> leaveConference(@PathVariable String conferenceId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            videoConferenceService.leaveConference(conferenceId, currentUserId);

            return ResponseEntity.ok(ApiResponse.success("离开会议成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 结束视频会议
     */
    @PostMapping("/{conferenceId}/end")
    public ResponseEntity<ApiResponse<Object>> endConference(@PathVariable String conferenceId) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            videoConferenceService.endConference(conferenceId, currentUserId);

            return ResponseEntity.ok(ApiResponse.success("会议结束成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取会议参与者列表
     */
    @GetMapping("/{conferenceId}/participants")
    public ResponseEntity<ApiResponse<Object>> getParticipants(@PathVariable String conferenceId) {
        try {
            var participants = videoConferenceService.getConferenceParticipants(conferenceId);
            var participantDTOs = participants.stream()
                    .map(VideoConferenceService.ParticipantDTO::new)
                    .toList();

            return ResponseEntity.ok(ApiResponse.success("获取参与者列表成功", participantDTOs));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取会议消息历史
     */
    @GetMapping("/{conferenceId}/messages")
    public ResponseEntity<ApiResponse<Object>> getMessageHistory(@PathVariable String conferenceId) {
        try {
            var messages = videoConferenceService.getMessageHistory(conferenceId);
            var messageDTOs = messages.stream()
                    .map(VideoConferenceService.ChatMessageDTO::new)
                    .toList();

            return ResponseEntity.ok(ApiResponse.success("获取消息历史成功", messageDTOs));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取文档的会议列表
     */
    @GetMapping("/document/{documentId}")
    public ResponseEntity<ApiResponse<Object>> getDocumentConferences(@PathVariable Long documentId) {
        try {
            var conferences = videoConferenceService.getDocumentConferences(documentId);

            return ResponseEntity.ok(ApiResponse.success("获取会议列表成功", conferences));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 切换屏幕共享状态
     */
    @PostMapping("/{conferenceId}/screen-sharing")
    public ResponseEntity<ApiResponse<Object>> toggleScreenSharing(
            @PathVariable String conferenceId,
            @RequestBody ScreenSharingRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            videoConferenceService.toggleScreenSharing(conferenceId, currentUserId, request.isSharing());

            return ResponseEntity.ok(ApiResponse.success("屏幕共享状态更新成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 切换音视频状态
     */
    @PostMapping("/{conferenceId}/media")
    public ResponseEntity<ApiResponse<Object>> toggleMedia(
            @PathVariable String conferenceId,
            @RequestBody MediaToggleRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            videoConferenceService.toggleMedia(conferenceId, currentUserId,
                    request.getVideoEnabled(), request.getAudioEnabled());

            return ResponseEntity.ok(ApiResponse.success("媒体状态更新成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * WebSocket: 发送聊天消息
     */
    @MessageMapping("/conference/{conferenceId}/send-message")
    @SendTo("/topic/conference/{conferenceId}/messages")
    public VideoConferenceService.ChatMessageDTO sendChatMessage(
            @DestinationVariable String conferenceId,
            @Payload ChatMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor) {
        Long userId = request.userId;
        var message = videoConferenceService.sendMessage(conferenceId, userId, request.getContent());

        return new VideoConferenceService.ChatMessageDTO(message);
    }

    /**
     * WebSocket: 发送视频帧（通过WebSocket传输视频数据）
     */
    @MessageMapping("/conference/{conferenceId}/video-frame")
    @SendTo("/topic/conference/{conferenceId}/video-frames")
    public VideoFrameDTO sendVideoFrame(
            @DestinationVariable String conferenceId,
            @Payload VideoFrameDTO request,
            SimpMessageHeaderAccessor headerAccessor) {

        Long userId = request.getUserId();

        // 验证用户是否在会议中
        try {
            var participant = videoConferenceService.getParticipant(conferenceId, userId);
            if (participant == null || participant.getStatus() != ParticipantStatus.JOINED) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }

        return request;
    }


    /**
     * WebSocket: 发送音频数据
     */
    @MessageMapping("/conference/{conferenceId}/audio-data")
    @SendTo("/topic/conference/{conferenceId}/audio-data")
    public AudioDataDTO sendAudioData(
            @DestinationVariable String conferenceId,
            @Payload AudioDataDTO request,
            SimpMessageHeaderAccessor headerAccessor) {

        Long userId = request.getUserId();

        // 验证用户是否在会议中
        try {
            var participant = videoConferenceService.getParticipant(conferenceId, userId);
            if (participant == null || participant.getStatus() != ParticipantStatus.JOINED) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }

        return request;
    }

    // 请求DTO类
    static class CreateConferenceRequest {
        private String title;
        private String description;
        private Integer maxParticipants;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getMaxParticipants() { return maxParticipants; }
        public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }
    }

    static class ChatMessageRequest {
        private String content;
        private Long userId;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Long getUserId() { return userId; }
    }

    static class ScreenSharingRequest {
        private Boolean sharing;

        public Boolean isSharing() { return sharing; }
        public void setSharing(Boolean sharing) { this.sharing = sharing; }
    }

    static class MediaToggleRequest {
        private Boolean videoEnabled;
        private Boolean audioEnabled;

        public Boolean getVideoEnabled() { return videoEnabled; }
        public void setVideoEnabled(Boolean videoEnabled) { this.videoEnabled = videoEnabled; }
        public Boolean getAudioEnabled() { return audioEnabled; }
        public void setAudioEnabled(Boolean audioEnabled) { this.audioEnabled = audioEnabled; }
    }
}