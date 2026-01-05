class CollaborationManager {
    constructor() {
        this.currentDocumentId = null;
        this.currentUserId = null;
        this.onlineUsers = new Map();
        this.cursorTimeouts = new Map();
        this.isCollaborator = false;
    }

    // 初始化协作功能
    async initialize(documentId) {
        this.currentDocumentId = documentId;

        // 获取当前用户信息
        const userResponse = await user.getProfile();
        if (userResponse.success) {
            this.currentUserId = userResponse.data.userId;
        }

        // 加入文档协作
        await this.joinDocument();

        // 设置WebSocket订阅
        this.setupWebSocketSubscriptions();

        // 加载在线用户
        await this.loadOnlineUsers();

        // 设置协作相关的事件监听器
        this.setupCollaborationEventListeners();
    }

    // 加入文档协作
    async joinDocument() {
        try {
            const sessionId = this.generateSessionId();
            const response = await collaborationAPI.joinDocument(this.currentDocumentId, sessionId);

            if (response.success) {
                console.log('成功加入文档协作');
                this.isCollaborator = true;
            }
        } catch (error) {
            console.error('加入文档协作失败:', error);
        }
    }

    // 离开文档协作
    async leaveDocument() {
        if (this.isCollaborator) {
            try {
                await collaborationAPI.leaveDocument(this.currentDocumentId);
                this.isCollaborator = false;
            } catch (error) {
                console.error('离开文档协作失败:', error);
            }
        }
    }

    // 设置WebSocket订阅
    setupWebSocketSubscriptions() {
        // 订阅内容更新
        collaborationSocket.subscribe(`/topic/document/${this.currentDocumentId}/content`, (message) => {
            this.handleContentUpdate(message);
        });

        // 订阅光标更新
        collaborationSocket.subscribe(`/topic/document/${this.currentDocumentId}/cursors`, (message) => {
            this.handleCursorUpdate(message);
        });

        // 订阅在线用户更新
        collaborationSocket.subscribe(`/topic/document/${this.currentDocumentId}/users`, (message) => {
            this.handleOnlineUsersUpdate(message);
        });

        // 订阅评论更新
        collaborationSocket.subscribe(`/topic/document/${this.currentDocumentId}/comments`, (message) => {
            if (window.commentManager) {
                window.commentManager.handleCommentsUpdate(message);
            }
        });

        // 订阅任务更新
        collaborationSocket.subscribe(`/topic/document/${this.currentDocumentId}/tasks`, (message) => {
            if (window.taskManager) {
                window.taskManager.handleTasksUpdate(message);
            }
        });

        // 订阅个人通知
        collaborationSocket.subscribe(`/topic/user/${this.currentUserId}/queue/notifications`, (message) => {
            if (window.notificationManager) {
                window.notificationManager.handleNewNotification(message);
            }
        });
    }

    // 处理内容更新
    handleContentUpdate(message) {
        // 忽略自己发送的更新
        if (message.userId === this.currentUserId) return;

        const content = message.content;

        // 根据文档类型更新内容
        if (window.currentDocumentType === 'RICH_TEXT' && window.quill) {
            const currentContent = window.quill.root.innerHTML;
            if (currentContent !== content) {
                window.quill.root.innerHTML = content;
            }
        } else if (window.currentDocumentType === 'MARKDOWN' && window.easyMDE) {
            const currentContent = window.easyMDE.value();
            if (currentContent !== content) {
                window.easyMDE.value(content);
                if (window.updateMarkdownPreview) {
                    window.updateMarkdownPreview();
                }
            }
        }
    }

    // 处理光标更新
    handleCursorUpdate(message) {
        // 忽略自己发送的光标更新
        if (message.userId === this.currentUserId) return;

        const position = message.position;
        const userId = message.userId;

        this.showUserCursor(userId, position);
    }

    // 显示用户光标
    showUserCursor(userId, position) {
        // 清除之前的定时器
        if (this.cursorTimeouts.has(userId)) {
            clearTimeout(this.cursorTimeouts.get(userId));
        }

        const user = this.onlineUsers.get(userId);
        if (!user) return;

        // 创建或更新光标指示器
        let cursorIndicator = document.getElementById(`cursor-${userId}`);
        if (!cursorIndicator) {
            cursorIndicator = document.createElement('div');
            cursorIndicator.id = `cursor-${userId}`;
            cursorIndicator.className = 'user-cursor';

            const avatarFileName = this.getFileNameFromPath(user.avatar_path);
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
                cursorIndicator.innerHTML = `
                    <div class="cursor-line"></div>
                    <div class="cursor-user">
                        <img src="${avatarUrl}" alt="${user.username}">
                        <span>${user.username}</span>
                    </div>
                `;
            } else {
                cursorIndicator.innerHTML = `
                    <div class="cursor-line"></div>
                    <div class="cursor-user">
                        <div class="avatar-placeholder">${user.username.charAt(0).toUpperCase()}</div>
                        <span>${user.username}</span>
                    </div>
                `;
            }
            document.querySelector('.editor-content').appendChild(cursorIndicator);
        }

        // 根据位置更新光标位置（这里需要根据具体的编辑器实现位置计算）
        this.updateCursorPosition(cursorIndicator, position, userId);

        // 2秒后隐藏光标
        const timeout = setTimeout(() => {
            cursorIndicator.style.display = 'none';
        }, 2000);

        this.cursorTimeouts.set(userId, timeout);
    }

    updateCursorPosition(cursorIndicator, position, userId) {
        if (!cursorIndicator || position === undefined || position === null) return;

        const user = this.onlineUsers.get(userId);
        if (!user) return;

        try {
            let topPosition = 0;
            let leftPosition = 0;

            if (window.currentDocumentType === 'RICH_TEXT' && window.quill) {
                console.log('富文本编辑器，位置:', position);

                try {
                    // 确保position是数字
                    const charPos = Number(position);
                    if (isNaN(charPos)) {
                        console.warn('位置不是有效的数字:', position);
                        return;
                    }

                    // 获取Quill编辑器的边界
                    const bounds = window.quill.getBounds(charPos);
                    console.log('Quill边界:', bounds);

                    if (bounds) {
                        // 获取编辑器容器
                        const editorElement = document.querySelector('#richTextEditor .ql-editor');
                        if (!editorElement) {
                            console.error('找不到富文本编辑器元素');
                            return;
                        }

                        // 获取编辑器容器的位置
                        const editorRect = editorElement.getBoundingClientRect();
                        const editorContentRect = document.querySelector('.editor-content').getBoundingClientRect();

                        // 计算相对于.editor-content的绝对位置
                        topPosition = bounds.top + (editorRect.top - editorContentRect.top);
                        leftPosition = bounds.left + (editorRect.left - editorContentRect.left);

                        console.log('计算出的位置 - top:', topPosition, 'left:', leftPosition);
                    }
                } catch (error) {
                    console.error('富文本位置计算错误:', error);
                    return;
                }

            } else if (window.currentDocumentType === 'MARKDOWN' && window.easyMDE) {
                  console.log('Markdown编辑器，位置:', position);

                  try {
                      const charPos = Number(position);
                      if (isNaN(charPos)) {
                          console.warn('位置不是有效的字符索引:', position);
                          return;
                      }

                      // 将字符索引转换为CodeMirror位置对象
                      const pos = window.easyMDE.codemirror.posFromIndex(charPos);
                      console.log('转换后的CodeMirror位置:', pos);

                      // 获取CodeMirror的坐标
                      const coords = window.easyMDE.codemirror.charCoords(pos, "local");
                      console.log('CodeMirror坐标:', coords);

                      if (coords) {
                          // 获取编辑器元素
                          const editorElement = window.easyMDE.codemirror.getWrapperElement();
                          const editorContentRect = document.querySelector('.editor-content').getBoundingClientRect();
                          const editorRect = editorElement.getBoundingClientRect();

                          // 计算相对于.editor-content的绝对位置
                          topPosition = coords.top + (editorRect.top - editorContentRect.top);
                          leftPosition = coords.left + (editorRect.left - editorContentRect.left);

                          console.log('计算出的位置 - top:', topPosition, 'left:', leftPosition);
                      }
                  } catch (error) {
                      console.error('Markdown位置计算错误:', error);
                      return;
                  }
            } else {
                console.log('回退方案，位置:', position);
                return;
            }

            // 设置光标指示器的位置
            cursorIndicator.style.top = `${topPosition}px`;
            cursorIndicator.style.left = `${leftPosition}px`;
            cursorIndicator.style.display = 'block';
            cursorIndicator.style.zIndex = '1000';

            console.log('最终位置 - top:', cursorIndicator.style.top, 'left:', cursorIndicator.style.left);

        } catch (error) {
            console.error('更新光标位置失败:', error);
        }
    }


    // 处理在线用户更新
    handleOnlineUsersUpdate(users) {
        this.onlineUsers.clear();
        users.forEach(user => {
            this.onlineUsers.set(user.id, user);
        });
        this.updateOnlineUsersDisplay();
    }

    // 更新在线用户显示
    updateOnlineUsersDisplay() {
        const onlineUsersContainer = document.getElementById('onlineUsers');
        if (!onlineUsersContainer) {
            return;
        }

        let html = '';
        this.onlineUsers.forEach((user, userId) => {
            const avatarFileName = this.getFileNameFromPath(user.avatar_path);
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
                    <div class="online-user" title="${user.username}">
                        <img src="${avatarUrl}" alt="${user.username}">
                        ${userId === this.currentUserId ? '<span style="color: black;">(我)</span>' : `<span style="color: black;">${user.username}</span>`}
                    </div>
                `;
            } else {
                html += `
                    <div class="online-user" title="${user.username}">
                        <div class="avatar-placeholder">${user.username.charAt(0).toUpperCase()}</div>
                        ${userId === this.currentUserId ? '<span style="color: black;">(我)</span>' : `<span style="color: black;">${user.username}</span>`}
                    </div>
                `;
            }
        });

        onlineUsersContainer.innerHTML = html;
    }

    // 从文件路径中提取文件名
    getFileNameFromPath(path) {
        if (!path) return '';
        return path.split('/').pop().split('\\').pop();
    }

    // 加载在线用户
    async loadOnlineUsers() {
        try {
            const response = await collaborationAPI.getOnlineUsers(this.currentDocumentId);
            if (response.success) {
                this.handleOnlineUsersUpdate(response.data.onlineUsers);
            }
        } catch (error) {
            console.error('加载在线用户失败:', error);
        }
    }

    // 发送内容更新
    sendContentUpdate(content) {
        if (!this.isCollaborator || !collaborationSocket.isConnected()) return;

        const message = {
            content: content,
            userId: this.currentUserId
        };

        collaborationSocket.send(`/app/document/${this.currentDocumentId}/content`, message);
    }

    // 发送光标更新
    sendCursorUpdate(position) {
        if (!this.isCollaborator || !collaborationSocket.isConnected()) return;

        const message = {
            position: position,
            userId: this.currentUserId
        };

        collaborationSocket.send(`/app/document/${this.currentDocumentId}/cursor`, message);
    }

    // 设置协作事件监听器
    setupCollaborationEventListeners() {
//        监听编辑器内容变化
//        if (window.quill) {
//            window.quill.on('text-change', () => {
//                const content = window.quill.root.innerHTML;
//                this.sendContentUpdate(content);
//            });
//
//            // 监听光标活动
//            window.quill.on('selection-change', (range, oldRange, source) => {
//                if (range && source === 'user') {
//                    // 计算光标位置（基于行号或字符位置）
//                    const position = this.calculateQuillCursorPosition(range);
//                    this.sendCursorUpdate(position);
//                }
//            });
//        }
//
//        if (window.easyMDE) {
//            window.easyMDE.codemirror.on('change', () => {
//                const content = window.easyMDE.value();
//                this.sendContentUpdate(content);
//            });
//
//            window.easyMDE.codemirror.on('cursorActivity', () => {
//                const cursor = window.easyMDE.codemirror.getCursor();
//                this.sendCursorUpdate(cursor.line);
//            });
//        }
    }

    // 计算Quill光标位置
    calculateQuillCursorPosition(range) {
        if (!range || !window.quill) return 0;

        try {
            // 使用字符位置作为光标标识
            // 这样可以精确到具体的字符位置，而不是行号
            return range.index;

        } catch (error) {
            console.error('计算光标位置失败:', error);
            return range.index; // 回退到字符位置
        }
    }

    // 生成会话ID
    generateSessionId() {
        return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }

    // 清理资源
    cleanup() {
        this.leaveDocument();
        this.onlineUsers.clear();
        this.cursorTimeouts.forEach(timeout => clearTimeout(timeout));
        this.cursorTimeouts.clear();
    }
}