const collaborationAPI = {
    // 加入文档协作
    async joinDocument(documentId, sessionId) {
        try {
            const response = await apiRequest(`/collaboration/${documentId}/join`, {
                method: 'POST',
                body: JSON.stringify({ sessionId })
            });
            return response;
        } catch (error) {
            console.error('加入文档协作错误:', error);
            throw error;
        }
    },

    // 离开文档协作
    async leaveDocument(documentId) {
        try {
            const response = await apiRequest(`/collaboration/${documentId}/leave`, {
                method: 'POST'
            });
            return response;
        } catch (error) {
            console.error('离开文档协作错误:', error);
            throw error;
        }
    },

    // 获取在线用户
        async getOnlineUsers(documentId) {
            try {
                const response = await apiRequest(`/collaboration/${documentId}/online-users`);
                return response;
            } catch (error) {
                console.error('获取在线用户错误:', error);
                throw error;
            }
        }
};

const commentAPI = {
    // 添加评论
    async addComment(documentId, content, position) {
        try {
            const response = await apiRequest(`/comments/${documentId}`, {
                method: 'POST',
                body: JSON.stringify({ content, position })
            });
            return response;
        } catch (error) {
            console.error('添加评论错误:', error);
            throw error;
        }
    },

    // 回复评论
    async replyToComment(commentId, content) {
        try {
            const response = await apiRequest(`/comments/${commentId}/reply`, {
                method: 'POST',
                body: JSON.stringify({ content })
            });
            return response;
        } catch (error) {
            console.error('回复评论错误:', error);
            throw error;
        }
    },

    // 标记评论为已解决
    async resolveComment(commentId) {
        try {
            const response = await apiRequest(`/comments/${commentId}/resolve`, {
                method: 'PUT'
            });
            return response;
        } catch (error) {
            console.error('标记评论解决错误:', error);
            throw error;
        }
    },

    // 删除评论
    async deleteComment(commentId) {
        try {
            const response = await apiRequest(`/comments/${commentId}`, {
                method: 'DELETE'
            });
            return response;
        } catch (error) {
            console.error('删除评论错误:', error);
            throw error;
        }
    },

    // 获取文档评论
    async getDocumentComments(documentId) {
        try {
            const response = await apiRequest(`/comments/document/${documentId}`);
            return response;
        } catch (error) {
            console.error('获取评论错误:', error);
            throw error;
        }
    }
};

const taskAPI = {
    // 创建任务
    async createTask(documentId, taskData) {
        try {
            const response = await apiRequest(`/tasks/${documentId}`, {
                method: 'POST',
                body: JSON.stringify(taskData)
            });
            return response;
        } catch (error) {
            console.error('创建任务错误:', error);
            throw error;
        }
    },

    // 更新任务状态
    async updateTaskStatus(taskId, status) {
        try {
            const response = await apiRequest(`/tasks/${taskId}/status`, {
                method: 'PUT',
                body: JSON.stringify({ status })
            });
            return response;
        } catch (error) {
            console.error('更新任务状态错误:', error);
            throw error;
        }
    },

    // 获取文档任务
    async getDocumentTasks(documentId) {
        try {
            const response = await apiRequest(`/tasks/document/${documentId}`);
            return response;
        } catch (error) {
            console.error('获取文档任务错误:', error);
            throw error;
        }
    },

    // 获取用户任务
    async getUserTasks() {
        try {
            const response = await apiRequest('/tasks/my-tasks');
            return response;
        } catch (error) {
            console.error('获取用户任务错误:', error);
            throw error;
        }
    },

    // 获取待处理任务
    async getPendingTasks() {
        try {
            const response = await apiRequest('/tasks/pending');
            return response;
        } catch (error) {
            console.error('获取待处理任务错误:', error);
            throw error;
        }
    }
};

const notificationAPI = {
    // 获取用户通知
    async getUserNotifications() {
        try {
            const response = await apiRequest('/notifications');
            return response;
        } catch (error) {
            console.error('获取通知错误:', error);
            throw error;
        }
    },

    // 标记通知为已读
    async markAsRead(notificationId) {
        try {
            const response = await apiRequest(`/notifications/${notificationId}/read`, {
                method: 'PUT'
            });
            return response;
        } catch (error) {
            console.error('标记通知已读错误:', error);
            throw error;
        }
    },

    // 标记所有通知为已读
    async markAllAsRead() {
        try {
            const response = await apiRequest('/notifications/mark-all-read', {
                method: 'PUT'
            });
            return response;
        } catch (error) {
            console.error('标记所有通知已读错误:', error);
            throw error;
        }
    },

    // 获取未读通知数量
    async getUnreadCount() {
        try {
            const response = await apiRequest('/notifications/unread-count');
            return response;
        } catch (error) {
            console.error('获取未读数量错误:', error);
            throw error;
        }
    },

    async deleteNotification(notificationId) {
        try {
            const response = await apiRequest(`/notifications/${notificationId}`, {
                method: 'DELETE'
            });
            return response;
        } catch (error) {
            console.error('删除通知错误: ', error);
            throw error;
        }
    }
};

// WebSocket管理类
class CollaborationWebSocket {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.subscriptions = new Map();
        this.pendingSubscriptions = new Map(); // 保存订阅信息
    }

    // 连接WebSocket
    connect() {
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);

        this.stompClient.connect({}, (frame) => {
            console.log('WebSocket连接成功:', frame);
            this.connected = true;

            // 连接成功后重新订阅所有之前的订阅
            this.resubscribeAll();
        }, (error) => {
            console.error('WebSocket连接失败:', error);
            this.connected = false;

            // 5秒后重连
            setTimeout(() => this.connect(), 5000);
        });
    }

    // 订阅主题
    subscribe(destination, callback) {
        // 保存订阅信息，即使未连接也可以保存
        this.pendingSubscriptions.set(destination, callback);

        if (!this.connected) {
            console.warn('WebSocket未连接，稍后自动订阅:', destination);
            return null;
        }

        const subscription = this.stompClient.subscribe(destination, (message) => {
            const parsedMessage = JSON.parse(message.body);
            callback(parsedMessage);
        });

        this.subscriptions.set(destination, subscription);
        return subscription;
    }

    // 取消订阅
    unsubscribe(destination) {
        // 从待订阅列表中移除
        this.pendingSubscriptions.delete(destination);

        const subscription = this.subscriptions.get(destination);
        if (subscription) {
            subscription.unsubscribe();
            this.subscriptions.delete(destination);
        }
    }

    // 重新订阅所有
    resubscribeAll() {
        // 重新订阅所有保存的订阅
        this.pendingSubscriptions.forEach((callback, destination) => {
            this.subscribe(destination, callback);
        });
    }

    // 发送消息
    send(destination, message) {
        if (!this.connected) {
            console.warn('WebSocket未连接，无法发送消息');
            return false;
        }

        this.stompClient.send(destination, {}, JSON.stringify(message));
        return true;
    }

    // 断开连接
    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect();
            this.connected = false;
            this.subscriptions.clear();
            // 注意：这里不清空 pendingSubscriptions，以便重连后重新订阅
        }
    }

    // 获取连接状态
    isConnected() {
        return this.connected;
    }
}

// 创建全局WebSocket实例
const collaborationSocket = new CollaborationWebSocket();