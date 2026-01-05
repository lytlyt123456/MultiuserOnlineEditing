const videoConferenceAPI = {
    // 创建视频会议
    async createConference(documentId, conferenceData) {
        try {
            const response = await apiRequest(`/video-conference/document/${documentId}`, {
                method: 'POST',
                body: JSON.stringify(conferenceData)
            });
            return response;
        } catch (error) {
            console.error('创建视频会议错误:', error);
            throw error;
        }
    },

    // 加入视频会议
    async joinConference(conferenceId) {
        try {
            const response = await apiRequest(`/video-conference/${conferenceId}/join`, {
                method: 'POST'
            });
            return response;
        } catch (error) {
            console.error('加入视频会议错误:', error);
            throw error;
        }
    },

    // 离开视频会议
    async leaveConference(conferenceId) {
        try {
            const response = await apiRequest(`/video-conference/${conferenceId}/leave`, {
                method: 'POST'
            });
            return response;
        } catch (error) {
            console.error('离开视频会议错误:', error);
            throw error;
        }
    },

    // 结束视频会议
    async endConference(conferenceId) {
        try {
            const response = await apiRequest(`/video-conference/${conferenceId}/end`, {
                method: 'POST'
            });
            return response;
        } catch (error) {
            console.error('结束视频会议错误:', error);
            throw error;
        }
    },

    // 获取会议参与者列表
    async getParticipants(conferenceId) {
        try {
            const response = await apiRequest(`/video-conference/${conferenceId}/participants`);
            return response;
        } catch (error) {
            console.error('获取参与者列表错误:', error);
            throw error;
        }
    },

    // 获取会议消息历史
    async getMessageHistory(conferenceId) {
        try {
            const response = await apiRequest(`/video-conference/${conferenceId}/messages`);
            return response;
        } catch (error) {
            console.error('获取消息历史错误:', error);
            throw error;
        }
    },

    // 获取文档的会议列表
    async getDocumentConferences(documentId) {
        try {
            const response = await apiRequest(`/video-conference/document/${documentId}`);
            return response;
        } catch (error) {
            console.error('获取会议列表错误:', error);
            throw error;
        }
    },

    // 切换屏幕共享状态
    async toggleScreenSharing(conferenceId, isSharing) {
        try {
            const response = await apiRequest(`/video-conference/${conferenceId}/screen-sharing`, {
                method: 'POST',
                body: JSON.stringify({ sharing: isSharing })
            });
            return response;
        } catch (error) {
            console.error('切换屏幕共享错误:', error);
            throw error;
        }
    },

    // 切换音视频状态
    async toggleMedia(conferenceId, videoEnabled, audioEnabled) {
        try {
            const response = await apiRequest(`/video-conference/${conferenceId}/media`, {
                method: 'POST',
                body: JSON.stringify({
                    videoEnabled: videoEnabled,
                    audioEnabled: audioEnabled
                })
            });
            return response;
        } catch (error) {
            console.error('切换媒体状态错误:', error);
            throw error;
        }
    }
};

class VideoConferenceManager {
    constructor() {
        this.currentDocumentId = null;
        this.currentConferenceId = null;
        this.currentUserId = null;
        this.isInConference = false;
		this.isSharingScreen = false;
		this.localVideoStream = null;
		this.localAudioStream = null;
		this.localScreenStream = null;
		this.remoteVideoFrames = new Map();
		this.remoteAudioBuffers = new Map();
		this.messages = [];
		this.subscribes = new Map();
		this.participants = new Map();

        // 视频捕获相关
        this.videoCaptureInterval = null;
        this.screenCaptureInterval = null;
        this.videoCanvas = null;
        this.videoContext = null;
        this.videoQuality = 0.1; // 视频质量（0.1 ~ 1.0）
        this.frameRate = 8;

        // 音频捕获相关
        this.audioCaptureInterval = null;
        this.audioContext = null;
        this.audioProcessor = null;

        // 音频播放相关
        this.audioContexts = new Map(); // 每个远程用户的音频上下文

        // 音视频状态
        this.isVideoEnabled = true;
        this.isAudioEnabled = true;
    }

    // 初始化视频会议管理器
    async initialize(documentId) {
        this.currentDocumentId = documentId;

        // 获取当前用户信息
        const userResponse = await user.getProfile();
        if (userResponse.success) {
            this.currentUserId = userResponse.data.userId;
        }

        // 设置全局WebSocket订阅
        this.setupGlobalWebSocketSubscriptions();

        // 加载文档的所有会议
        await this.loadDocumentConferences();
    }

    // 设置全局WebSocket订阅
    setupGlobalWebSocketSubscriptions() {
        // 文档会议更新订阅，一开始就订阅
        collaborationSocket.subscribe(`/topic/document/${this.currentDocumentId}/conferences`, (message) => {
            this.handleConferencesUpdate(message);
        });
    }

    // 设置会议相关的WebSocket订阅（在加入会议后调用）
    setupConferenceWebSocketSubscriptions() {
        if (!this.currentConferenceId) return;

        // 参与者更新
        if (!this.subscribes.has(`${this.currentConferenceId}_participants`)) {
            collaborationSocket.subscribe(`/topic/conference/${this.currentConferenceId}/participants`, (message) => {
                this.handleParticipantsUpdate(message);
            });
            this.subscribes.set(`${this.currentConferenceId}_participants`, '1');
        }

        // 聊天消息
        if (!this.subscribes.has(`${this.currentConferenceId}_messages`)) {
            collaborationSocket.subscribe(`/topic/conference/${this.currentConferenceId}/messages`, (message) => {
                this.handleChatMessage(message);
            });
            this.subscribes.set(`${this.currentConferenceId}_messages`, '1');
        }

        // 屏幕共享状态
        if (!this.subscribes.has(`${this.currentConferenceId}_screen-sharing`)) {
            collaborationSocket.subscribe(`/topic/conference/${this.currentConferenceId}/screen-sharing`, (message) => {
                this.handleScreenSharingUpdate(message);
            });
            this.subscribes.set(`${this.currentConferenceId}_screen-sharing`, '1');
        }

        // 媒体状态更新
        if (!this.subscribes.has(`${this.currentConferenceId}_media-status`)) {
            collaborationSocket.subscribe(`/topic/conference/${this.currentConferenceId}/media-status`, (message) => {
                this.handleMediaStatusUpdate(message);
            });
            this.subscribes.set(`${this.currentConferenceId}_media-status`, '1');
        }

        // 会议结束
        if (!this.subscribes.has(`${this.currentConferenceId}_ended`)) {
            collaborationSocket.subscribe(`/topic/conference/${this.currentConferenceId}/ended`, async (message) => {
                await this.handleConferenceEnded(message);
            });
            this.subscribes.set(`${this.currentConferenceId}_ended`, '1');
        }

        // 视频帧
        if (!this.subscribes.has(`user_${this.currentUserId}_video-frames`)) {
            collaborationSocket.subscribe(`/topic/conference/${this.currentConferenceId}/video-frames`, (message) => {
                this.handleVideoFrames(message);
            });
            this.subscribes.set(`user_${this.currentUserId}_video-frames`, '1');
        }

        // 音频数据
        if (!this.subscribes.has(`user_${this.currentUserId}_audio-data`)) {
            collaborationSocket.subscribe(`/topic/conference/${this.currentConferenceId}/audio-data`, (message) => {
                this.handleAudioData(message);
            });
            this.subscribes.set(`user_${this.currentUserId}_audio-data`, '1');
        }
    }

    // 加载文档的所有会议
    async loadDocumentConferences() {
        try {
            const response = await videoConferenceAPI.getDocumentConferences(this.currentDocumentId);
            if (response.success) {
                this.updateAvailableConferences(response.data);
            }
        } catch (error) {
            console.error('加载文档会议失败:', error);
        }
    }

    // 创建会议
    async createConference(title, description, maxParticipants) {
        try {
            const conferenceData = {
                title: title,
                description: description,
                maxParticipants: maxParticipants
            };

            const response = await videoConferenceAPI.createConference(this.currentDocumentId, conferenceData);
            if (response.success) {
                // 自动加入创建的会议
                // await this.joinConference(response.data.conferenceId);
                this.currentConferenceId = response.data.conferenceId;
                this.isInConference = true;

                // 设置会议相关的WebSocket订阅
                this.setupConferenceWebSocketSubscriptions();

                // 初始化本地媒体流
                await this.initializeLocalMedia();

                // 加载会议数据
                await this.loadConferenceData();

                // 显示会议界面
                this.showConferenceInterface();

                // 开始捕获和发送音视频
                this.startMediaCapture();

                return true;
            }
            return false;
        } catch (error) {
            console.error('创建会议失败:', error);
            alert('创建会议失败: ' + error.message);
            return false;
        }
    }

    // 加入会议
    async joinConference(conferenceId) {
        try {
            const response = await videoConferenceAPI.joinConference(conferenceId);
            if (response.success) {
                this.currentConferenceId = conferenceId;
                this.isInConference = true;

                // 加载会议数据
                await this.loadConferenceData();

                // 设置会议相关的WebSocket订阅
                this.setupConferenceWebSocketSubscriptions();

                // 初始化本地媒体流
                await this.initializeLocalMedia();

                // 显示会议界面
                this.showConferenceInterface();

                // 开始捕获和发送音视频
                this.startMediaCapture();

                return true;
            }
            return false;
        } catch (error) {
            console.error('加入会议失败:', error);
            alert('加入会议失败: ' + error.message);
            return false;
        }
    }

    // 初始化本地媒体
    async initializeLocalMedia() {
        try {
            // 获取摄像头视频流
            if (this.isVideoEnabled) {
                this.localVideoStream = await navigator.mediaDevices.getUserMedia({
                    video: {
                        width: { ideal: 640 },
                        height: { ideal: 480 },
                        frameRate: { ideal: this.frameRate }
                    }
                });
            }

            // 获取麦克风音频流
            if (this.isAudioEnabled) {
                this.localAudioStream = await navigator.mediaDevices.getUserMedia({
                    audio: {
                        sampleRate: 44100,
                        channelCount: 1,
                        echoCancellation: true,
                        noiseSuppression: true
                    }
                });
            }

            // 显示本地视频
            this.displayLocalVideo();

            // 创建视频捕获画布
            this.createVideoCaptureCanvas();

        } catch (error) {
            console.error('获取媒体设备失败:', error);
            alert('无法访问摄像头或麦克风: ' + error.message);
        }
    }

    // 创建视频捕获画布
    createVideoCaptureCanvas() {
        this.videoCanvas = document.createElement('canvas');
        this.videoCanvas.width = 1280;
        this.videoCanvas.height = 960;
        this.videoContext = this.videoCanvas.getContext('2d');
    }

    // 开始媒体捕获
    startMediaCapture() {
        // 开始视频捕获
        if (this.isVideoEnabled && this.localVideoStream) {
            this.startVideoCapture();
        }

        // 开始音频捕获
        if (this.isAudioEnabled && this.localAudioStream) {
            this.startAudioCapture();
        }
    }

    // 开始视频捕获和发送
    startVideoCapture() {
        if (this.videoCaptureInterval) {
            clearInterval(this.videoCaptureInterval);
        }

        const videoTrack = this.localVideoStream.getVideoTracks()[0];
        if (!videoTrack) return;

        const videoElement = document.createElement('video');
        videoElement.srcObject = new MediaStream([videoTrack]);
        videoElement.play();

        this.videoCaptureInterval = setInterval(() => {
            if (!this.videoCanvas || !this.videoContext) return;

            try {
                // 绘制视频帧到画布
                this.videoContext.drawImage(videoElement, 0, 0,
                    this.videoCanvas.width, this.videoCanvas.height);

                // 获取Base64编码的图像数据
                const frameData = this.videoCanvas.toDataURL('image/jpeg', this.videoQuality);

                // 发送视频帧
                collaborationSocket.send(`/app/conference/${this.currentConferenceId}/video-frame`, {
                    userId: this.currentUserId,
                    frameData: frameData,
                    timestamp: Date.now(),
                    width: this.videoCanvas.width,
                    height: this.videoCanvas.height
                });

            } catch (error) {
                console.error('视频捕获失败:', error);
            }
        }, 1000 / this.frameRate);
    }

    // 开始屏幕视频捕获和发送
    startScreenCapture() {
        if (this.screenCaptureInterval) {
            clearInterval(this.screenCaptureInterval);
        }

        const screenTrack = this.localScreenStream.getVideoTracks()[0];
        if (!screenTrack) return;

        const videoElement = document.createElement('video');
        videoElement.srcObject = new MediaStream([screenTrack]);
        videoElement.play();

        this.screenCaptureInterval = setInterval(() => {
            if (!this.videoCanvas || !this.videoContext) return;

            try {
                // 绘制视频帧到画布
                this.videoContext.drawImage(videoElement, 0, 0,
                    this.videoCanvas.width, this.videoCanvas.height);

                // 获取Base64编码的图像数据
                const frameData = this.videoCanvas.toDataURL('image/jpeg', this.videoQuality);

                // 发送视频帧
                collaborationSocket.send(`/app/conference/${this.currentConferenceId}/video-frame`, {
                    userId: this.currentUserId,
                    frameData: frameData,
                    timestamp: Date.now(),
                    width: this.videoCanvas.width,
                    height: this.videoCanvas.height
                });

            } catch (error) {
                console.error('视频捕获失败:', error);
            }
        }, 1000 / this.frameRate);
    }

    // 开始音频捕获和发送
    startAudioCapture() {
        if (!this.localAudioStream || !window.AudioContext) return;

        try {
            // 创建音频上下文
            this.audioContext = new (window.AudioContext || window.webkitAudioContext)({
                sampleRate: 44100
            });

            // 创建音频处理节点
            const source = this.audioContext.createMediaStreamSource(this.localAudioStream);
            const processor = this.audioContext.createScriptProcessor(4096, 1, 1);

            processor.onaudioprocess = (event) => {
                const inputData = event.inputBuffer.getChannelData(0);

                // 将Float32Array转换为Int16Array
                const int16Array = this.floatTo16BitPCM(inputData);

                // 将Int16Array转换为Base64
                const base64String = this.arrayBufferToBase64(int16Array.buffer);

                // 发送音频数据
                collaborationSocket.send(`/app/conference/${this.currentConferenceId}/audio-data`, {
                    userId: this.currentUserId,
                    audioData: base64String,
                    sampleRate: this.audioContext.sampleRate,
                    channels: 1
                });
            };

            source.connect(processor);
            processor.connect(this.audioContext.destination);

            this.audioProcessor = processor;

        } catch (error) {
            console.error('音频捕获失败:', error);
        }
    }

    // Float32Array 转 Int16Array
    floatTo16BitPCM(float32Array) {
        const buffer = new ArrayBuffer(float32Array.length * 2);
        const view = new DataView(buffer);
        let offset = 0;

        for (let i = 0; i < float32Array.length; i++, offset += 2) {
            let s = Math.max(-1, Math.min(1, float32Array[i]));
            view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7FFF, true);
        }

        return new Int16Array(buffer);
    }

    // ArrayBuffer 转 Base64
    arrayBufferToBase64(buffer) {
        let binary = '';
        const bytes = new Uint8Array(buffer);
        const len = bytes.byteLength;
        for (let i = 0; i < len; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return window.btoa(binary);
    }

    // Base64 转 ArrayBuffer
    base64ToArrayBuffer(base64) {
        const binaryString = window.atob(base64);
        const len = binaryString.length;
        const bytes = new Uint8Array(len);
        for (let i = 0; i < len; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }
        return bytes.buffer;
    }

    // 显示本地视频
    displayLocalVideo() {
        const videoContainer = document.getElementById('videoContainer');
        if (!videoContainer || !this.localVideoStream) return;

        // 创建本地视频元素
        const localVideo = document.createElement('video');
        localVideo.id = 'localVideo';
        localVideo.srcObject = this.localVideoStream;
        localVideo.autoplay = true;
        localVideo.muted = true;
        localVideo.playsInline = true;
        localVideo.style.width = '640px';
        localVideo.style.height = '480px';

        const videoWrapper = document.createElement('div');
        videoWrapper.className = 'video-wrapper local-video';
        videoWrapper.innerHTML = `
            <div class="video-label">我</div>
        `;
        videoWrapper.appendChild(localVideo);

        videoContainer.appendChild(videoWrapper);
    }

    // 加载会议数据
    async loadConferenceData() {
        if (!this.currentConferenceId) return;

        try {
            // 加载参与者列表
            const participantsResponse = await videoConferenceAPI.getParticipants(this.currentConferenceId);
            if (participantsResponse.success) {
                this.handleParticipantsUpdate(participantsResponse.data);
            }

            // 加载消息历史
            const messagesResponse = await videoConferenceAPI.getMessageHistory(this.currentConferenceId);
            if (messagesResponse.success) {
                this.messages = messagesResponse.data;
                this.displayMessages();
            }
        } catch (error) {
            console.error('加载会议数据失败:', error);
        }
    }


    // 离开会议
    async leaveConference() {
        if (!this.currentConferenceId) return;

        try {
            // 停止媒体捕获
            this.stopMediaCapture();

            // 清理远程视频
            this.cleanupRemoteVideos();

            // 清理音频
            this.cleanupAudio();

            // 清理本地视频
            this.cleanupLocalVideo();

            // 清理视频容器
            this.cleanupVideoContainer();

            // 调用离开API
            await videoConferenceAPI.leaveConference(this.currentConferenceId);

            // 重置状态
            this.resetConferenceState();

            // 隐藏会议界面
            this.hideConferenceInterface();

        } catch (error) {
            console.error('离开会议失败:', error);
        }
    }

    // 新增：清理本地视频
    cleanupLocalVideo() {
        const localVideo = document.getElementById('localVideo');
        if (localVideo) {
            // 停止视频流
            if (localVideo.srcObject) {
                const stream = localVideo.srcObject;
                const tracks = stream.getTracks();
                tracks.forEach(track => track.stop());
                localVideo.srcObject = null;
            }

            // 移除视频元素
            const videoWrapper = localVideo.closest('.video-wrapper.local-video');
            if (videoWrapper && videoWrapper.parentNode) {
                videoWrapper.parentNode.removeChild(videoWrapper);
            }
        }
    }

    // 新增：清理视频容器
    cleanupVideoContainer() {
        const videoContainer = document.getElementById('videoContainer');
        if (videoContainer) {
            // 清理所有视频元素
            videoContainer.innerHTML = '';

            // 或者更精确的清理
            // const videoWrappers = videoContainer.querySelectorAll('.video-wrapper');
            // videoWrappers.forEach(wrapper => wrapper.remove());
        }
    }

    // 停止媒体捕获
    stopMediaCapture() {
        // 停止视频捕获
        if (this.videoCaptureInterval) {
            clearInterval(this.videoCaptureInterval);
            this.videoCaptureInterval = null;
        }

        // 停止音频捕获
        if (this.audioProcessor) {
            this.audioProcessor.disconnect();
            this.audioProcessor = null;
        }

        // 停止屏幕视频捕获
        if (this.screenCaptureInterval) {
            clearInterval(this.screenCaptureInterval);
            this.screenCaptureInterval = null;
        }

        // 关闭视频上下文
        if (this.videoContext) {
            this.videoContext = null;
        }

        // 关闭音频上下文
        if (this.audioContext) {
            this.audioContext.close();
            this.audioContext = null;
        }

        // 停止本地媒体流
        if (this.localVideoStream) {
            this.localVideoStream.getTracks().forEach(track => track.stop());
            this.localVideoStream = null;
        }

        if (this.localAudioStream) {
            this.localAudioStream.getTracks().forEach(track => track.stop());
            this.localAudioStream = null;
        }

        if (this.localScreenStream) {
            this.localScreenStream.getTracks().forEach(track => track.stop());
        }
    }

    // 清理远程视频
    cleanupRemoteVideos() {
        this.remoteVideoFrames.forEach((data, userId) => {
            if (data.canvas && data.canvas.parentNode) {
                data.canvas.parentNode.remove();
            }
        });
        this.remoteVideoFrames.clear();
    }

    // 清理音频
    cleanupAudio() {
        this.remoteAudioBuffers.forEach((data, userId) => {
            if (data.audioContext) {
                data.audioContext.close();
            }
        });
        this.remoteAudioBuffers.clear();
        this.audioContexts.clear();
    }

    // 结束会议
    async endConference() {
        if (!this.currentConferenceId) return;

        try {
            await videoConferenceAPI.endConference(this.currentConferenceId);
            // await this.leaveConference();
        } catch (error) {
            console.error('结束会议失败:', error);
            alert('结束会议失败: ' + error.message);
        }
    }

    // 发送聊天消息
    async sendChatMessage(content) {
        if (!this.currentConferenceId || !content.trim()) return;

        try {
            collaborationSocket.send(`/app/conference/${this.currentConferenceId}/send-message`, {
                content: content.trim(),
                userId: this.currentUserId
            });
        } catch (error) {
            console.error('发送消息失败:', error);
        }
    }

    async toggleVideo() {
        if (!this.isVideoEnabled && this.isSharingScreen) {
            return;
        }

        this.isVideoEnabled = !this.isVideoEnabled;

        if (this.isVideoEnabled) {
            this.localVideoStream.getVideoTracks()[0].enabled = true;
            // 重新开始视频捕获
            this.startVideoCapture();
        } else {
            // 停止视频捕获
            if (this.videoCaptureInterval) {
                clearInterval(this.videoCaptureInterval);
                this.videoCaptureInterval = null;
            }

            // 停止视频流
            this.localVideoStream.getTracks()[0].enabled = false;
        }

        // 更新UI
        const videoToggle = document.getElementById('videoToggle');
        if (videoToggle) {
            videoToggle.textContent = this.isVideoEnabled ? '📹 关闭视频' : '📹 打开视频';
            videoToggle.className = this.isVideoEnabled ? 'btn btn-secondary' : 'btn btn-primary';
        }

        // 通知其他参与者
        try {
            await videoConferenceAPI.toggleMedia(
                this.currentConferenceId,
                this.isVideoEnabled,
                null
            );
        } catch (error) {
            console.error('通知媒体状态失败:', error);
        }
    }

    // 切换音频状态
    async toggleAudio() {
        this.isAudioEnabled = !this.isAudioEnabled;

        if (this.isAudioEnabled) {
            // 重新获取音频流
            this.localAudioStream.getAudioTracks()[0].enabled = true;
            // 重新开始音频捕获
            this.startAudioCapture();
        } else {
            // 停止音频捕获
            if (this.audioProcessor) {
                this.audioProcessor.disconnect();
                this.audioProcessor = null;
            }
            // 停止音频流
            this.localAudioStream.getAudioTracks()[0].enabled = false;
        }

        // 更新UI
        const audioToggle = document.getElementById('audioToggle');
        if (audioToggle) {
            audioToggle.textContent = this.isAudioEnabled ? '🎤 静音' : '🎤 音频';
        }

        // 通知其他参与者
        await videoConferenceAPI.toggleMedia(
            this.currentConferenceId,
            null,
            this.isAudioEnabled
        );
    }

    // 切换屏幕共享
    async toggleScreenSharing() {
        try {
            if (!this.isSharingScreen) {
                // 开始屏幕共享
                this.localScreenStream = await navigator.mediaDevices.getDisplayMedia({
                    video: true,
                    audio: false
                });

                this.isSharingScreen = true;
                // 关闭视频
                if (this.isVideoEnabled) {
                    await toggleVideo();
                }

                // 更新本地视频显示
                const localVideo = document.getElementById('localVideo');
                if (localVideo) {
                    localVideo.srcObject = this.localScreenStream;
                }

                if (this.videoCaptureInterval) {
                    clearInterval(this.videoCaptureInterval);
                    this.videoCaptureInterval = null;
                }

                // 开始视频捕获
                this.startScreenCapture();

                // 处理屏幕共享结束
                this.localScreenStream.getVideoTracks()[0].onended = () => {
                    if (this.isSharingScreen) {
                        this.toggleScreenSharing();
                    }
                };

                // 更新UI
                const screenShareToggle = document.getElementById('screenShareToggle');
                if (screenShareToggle) {
                    screenShareToggle.textContent = '🖥️ 停止共享';
                    screenShareToggle.classList.add('sharing');
                }

            } else {
                this.isSharingScreen = false;

                // 停止屏幕共享，恢复摄像头
                if (this.screenCaptureInterval) {
                    clearInterval(this.screenCaptureInterval);
                    this.screenCaptureInterval = null;
                }

                if (this.localScreenStream) {
                    this.localScreenStream.getVideoTracks()[0].enabled = false;
                    this.localScreenStream = null;
                }

                // 更新本地视频显示
                const localVideo = document.getElementById('localVideo');
                if (localVideo) {
                    localVideo.srcObject = this.localVideoStream;
                }

                // 重新开始视频捕获
                if (this.isVideoEnabled) {
                    this.startVideoCapture();
                }

                // 更新UI
                const screenShareToggle = document.getElementById('screenShareToggle');
                if (screenShareToggle) {
                    screenShareToggle.textContent = '🖥️ 共享屏幕';
                    screenShareToggle.classList.remove('sharing');
                }
            }

            // 通知其他参与者
            await videoConferenceAPI.toggleScreenSharing(this.currentConferenceId, this.isSharingScreen);

        } catch (error) {
            console.error('屏幕共享失败:', error);
        }
    }

    // 处理视频帧
    handleVideoFrames(message) {
        const { userId, frameData, timestamp, width, height } = message;

        if (userId === this.currentUserId) return; // 忽略自己的视频帧

        // 获取或创建远程视频容器
        let videoData = this.remoteVideoFrames.get(userId);
        if (!videoData) {
            videoData = this.createRemoteVideoContainer(userId);
            this.remoteVideoFrames.set(userId, videoData);
        }

        // 检查是否需要更新帧（避免重复绘制）
        if (timestamp <= videoData.lastTimestamp) return;

        // 更新视频帧
        this.updateRemoteVideoFrame(userId, frameData, width, height, timestamp);
    }

    // 创建远程视频容器
    createRemoteVideoContainer(userId) {
		let username = '';
		if (this.participants.has(userId)) {
			let p = this.participants.get(userId);
			username = p.username;
		}

        const videoContainer = document.getElementById('videoContainer');
        if (!videoContainer) return null;

        // 创建画布元素
        const canvas = document.createElement('canvas');
        canvas.width = 640;
        canvas.height = 480;
        canvas.className = 'remote-video-canvas';

        // 创建容器
        const wrapper = document.createElement('div');
        wrapper.className = 'video-wrapper remote-video';
        wrapper.innerHTML = `
            <div class="video-label">${username}</div>
        `;
        wrapper.appendChild(canvas);

        videoContainer.appendChild(wrapper);

        return {
            canvas: canvas,
            context: canvas.getContext('2d'),
            lastFrame: null,
            lastTimestamp: 0
        };
    }

    // 更新远程视频帧
    updateRemoteVideoFrame(userId, frameData, width, height, timestamp) {
        const videoData = this.remoteVideoFrames.get(userId);
        if (!videoData || !videoData.canvas) return;

        // 创建Image对象加载Base64图像
        const img = new Image();
        img.onload = () => {
            // 绘制到画布
            videoData.context.clearRect(0, 0, videoData.canvas.width, videoData.canvas.height);
            videoData.context.drawImage(img, 0, 0, videoData.canvas.width, videoData.canvas.height);

            // 更新最后帧信息
            videoData.lastFrame = frameData;
            videoData.lastTimestamp = timestamp;
        };
        img.src = frameData;
    }

    // 处理音频数据
    handleAudioData(message) {
        const { userId, audioData, sampleRate, channels } = message;

        if (userId === this.currentUserId) return; // 忽略自己的音频

        // 获取或创建音频缓冲区
        let audioBufferData = this.remoteAudioBuffers.get(userId);
        if (!audioBufferData) {
            audioBufferData = this.createAudioBuffer(userId, sampleRate, channels);
            this.remoteAudioBuffers.set(userId, audioBufferData);
        }

        // 解码和播放音频
        this.playAudioData(userId, audioData, sampleRate, channels);
    }

    // 创建音频缓冲区
    createAudioBuffer(userId, sampleRate, channels) {
        // 创建音频上下文
        const audioContext = new (window.AudioContext || window.webkitAudioContext)({
            sampleRate: sampleRate
        });

        return {
            audioContext: audioContext,
            audioBuffer: null,
            lastAudioData: null,
            sampleRate: sampleRate,
            channels: channels
        };
    }

    // 播放音频数据
    playAudioData(userId, audioData, sampleRate, channels) {
        const audioBufferData = this.remoteAudioBuffers.get(userId);
        if (!audioBufferData || !audioBufferData.audioContext) return;

        try {
            // 解码Base64音频数据
            const audioBuffer = this.base64ToArrayBuffer(audioData);
            const int16Array = new Int16Array(audioBuffer);

            // 转换为Float32Array
            const float32Array = new Float32Array(int16Array.length);
            for (let i = 0; i < int16Array.length; i++) {
                float32Array[i] = int16Array[i] / 32768.0;
            }

            // 创建音频缓冲区
            const buffer = audioBufferData.audioContext.createBuffer(
                channels,
                float32Array.length / channels,
                sampleRate
            );

            // 填充音频数据
            for (let channel = 0; channel < channels; channel++) {
                const channelData = buffer.getChannelData(channel);
                for (let i = 0; i < channelData.length; i++) {
                    channelData[i] = float32Array[i * channels + channel];
                }
            }

            // 创建音频源并播放
            const source = audioBufferData.audioContext.createBufferSource();
            source.buffer = buffer;
            source.connect(audioBufferData.audioContext.destination);
            source.start();

        } catch (error) {
            console.error('音频播放失败:', error);
        }
    }

    // 处理参与者更新
    handleParticipantsUpdate(participants) {
        this.participants.clear();
        participants.forEach(participant => {
            this.participants.set(participant.userId, participant);
        });

        this.displayParticipants();

        // 清理不存在的参与者的视频
        this.cleanupStaleVideoContainers();
    }

    // 清理过期的视频容器
    cleanupStaleVideoContainers() {
        const currentUserIds = new Set(this.participants.keys());

        this.remoteVideoFrames.forEach((data, userId) => {
            if (!currentUserIds.has(userId) || userId === this.currentUserId) {
                if (data.canvas && data.canvas.parentNode) {
                    data.canvas.parentNode.remove();
                }
                this.remoteVideoFrames.delete(userId);
            }
        });
    }

    // 处理聊天消息
    handleChatMessage(message) {
        this.messages.push(message);
        this.displayMessages();
    }

    // 处理屏幕共享更新
    handleScreenSharingUpdate(message) {
        const participant = this.participants.get(message.userId);
        if (participant) {
            participant.isSharingScreen = message.isSharing;
            this.displayParticipants();
        }
    }

    // 处理媒体状态更新
    handleMediaStatusUpdate(message) {
        const participant = this.participants.get(message.userId);
        if (participant) {
            if (message.videoEnabled !== undefined) {
                participant.isVideoEnabled = message.videoEnabled;
            }
            if (message.audioEnabled !== undefined) {
                participant.isAudioEnabled = message.audioEnabled;
            }
            this.displayParticipants();
        }
    }

    // 处理会议结束
    async handleConferenceEnded(message) {
        alert('会议已结束');
        await this.leaveConference();
    }

    // 处理会议列表更新
    handleConferencesUpdate(conferences) {
        this.updateAvailableConferences(conferences);
    }

    // 显示参与者列表
    displayParticipants() {
        const participantsList = document.getElementById('participantsList');
        if (!participantsList) return;

        let html = '';
        this.participants.forEach((participant, userId) => {
            const statusIcons = [];
            if (!participant.isVideoEnabled) statusIcons.push('📹❌');
            if (!participant.isAudioEnabled) statusIcons.push('🎤❌');
            if (participant.isSharingScreen) statusIcons.push('🖥️');

            const avatarFileName = this.getFileNameFromPath(participant.avatarPath);
            let avatarUrl = '';
            if (avatarFileName) {
                let host = '';
                if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
					host = 'http://localhost:8080';
				} else {
					host = `${window.location.protocol}//${window.location.host}`;
				}
                avatarUrl = `${host}/uploads/avatars/${avatarFileName}`;
            }

            if (avatarUrl) {
                html += `
                    <div class="participant-item">
                        <div class="participant-avatar">
                            <img src="${avatarUrl}" alt="${participant.username}" class="avatar-image">
                        </div>
                        <div class="participant-info">
                            <div class="participant-name">
                                ${participant.username}
                                ${userId === this.currentUserId ? '(我)' : ''}
                                ${participant.role === 'HOST' ? '👑' : ''}
                            </div>
                            <div class="participant-status">
                                ${statusIcons.join(' ')}
                            </div>
                        </div>
                    </div>
                `;
            } else {
                html += `
                    <div class="participant-item">
                        <div class="participant-avatar">
                            ${participant.username.charAt(0).toUpperCase()}
                        </div>
                        <div class="participant-info">
                            <div class="participant-name">
                                ${participant.username}
                                ${userId === this.currentUserId ? '(我)' : ''}
                                ${participant.role === 'HOST' ? '👑' : ''}
                            </div>
                            <div class="participant-status">
                                ${statusIcons.join(' ')}
                            </div>
                        </div>
                    </div>
                `;
            }
        });

        participantsList.innerHTML = html;
    }

    // 显示聊天消息
    displayMessages() {
        const chatMessages = document.getElementById('chatMessages');
        if (!chatMessages) {
            return;
        }

        let html = '';
        this.messages.forEach(message => {
            const isSystem = !message.userId;
            const isOwn = message.userId == this.currentUserId;

            html += `
                <div class="chat-message ${isSystem ? 'system' : ''} ${isOwn ? 'own' : ''}">
                    ${!isSystem ? `<div class="message-sender">${message.username}</div>` : ''}
                    <div class="message-content">${message.content}</div>
                    <div class="message-time">${new Date(message.sentAt).toLocaleTimeString()}</div>
                </div>
            `;
        });

        chatMessages.innerHTML = html;
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    // 更新可用会议列表
    updateAvailableConferences(conferences) {
        const availableConferences = document.getElementById('availableConferences');
        const noConferences = document.getElementById('noConferences');

        if (!availableConferences || !noConferences) return;

        if (!conferences || conferences.length === 0) {
            availableConferences.style.display = 'none';
            noConferences.style.display = 'block';
            return;
        }

        availableConferences.style.display = 'block';
        noConferences.style.display = 'none';

        let html = '';
        conferences.forEach(conference => {
            let count = 0;
            if (conference.participants) {
                conference.participants.forEach(participant => {
                    if (participant.status === 'JOINED') {
                        count += 1;
                    }
                });
            }
            html += `
                <div class="conference-item">
                    <div class="conference-info">
                        <div class="conference-title">${conference.title}</div>
                        <div class="conference-conferenceId">${conference.conferenceId}</div>
                        <div class="conference-meta">
                            创建者: ${conference.createdBy.username} |
                            人数: ${conference.participants ? count : 0}/${conference.maxParticipants}
                        </div>
                        ${conference.description ? `<div class="conference-description">${conference.description}</div>` : ''}
                    </div>
                    <button class="btn btn-primary" onclick="videoConferenceManager.joinConference('${conference.conferenceId}')">
                        加入会议
                    </button>
                </div>
            `;
        });

        availableConferences.innerHTML = html;
    }

    getFileNameFromPath(path) {
        if (!path) return '';
        return path.split('/').pop().split('\\').pop();
    }

    // 显示会议界面
    showConferenceInterface() {
        const panel = document.getElementById('videoConferencePanel');
        if (panel) {
            panel.classList.add('active');
        }

        const title = document.getElementById('conferenceTitle');
        if (title) {
            title.textContent = '视频会议 - 进行中';
        }
    }

    // 隐藏会议界面
    hideConferenceInterface() {
        const panel = document.getElementById('videoConferencePanel');
        if (panel) {
            panel.classList.remove('active');
        }
    }

    // 重置会议状态
    resetConferenceState() {
        this.currentConferenceId = null;
        this.isInConference = false;
        this.isSharingScreen = false;
        this.isVideoEnabled = true;
        this.isAudioEnabled = true;
        this.participants.clear();
        this.messages = [];

        const screenShareToggle = document.getElementById('screenShareToggle');
        if (screenShareToggle) {
            screenShareToggle.textContent = '🖥️ 共享屏幕';
            screenShareToggle.classList.remove('sharing');
        }

        const audioToggle = document.getElementById('audioToggle');
        if (audioToggle) {
            audioToggle.textContent = '🎤 静音';
        }

        const videoToggle = document.getElementById('videoToggle');
        if (videoToggle) {
            videoToggle.textContent = '📹 关闭视频';
        }
    }
}