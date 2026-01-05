class NotificationManager {
    constructor() {
        this.notifications = [];
        this.unreadCount = 0;
    }

    // 初始化通知功能
    async initialize() {
        this.initializeNotificationUI();
        await this.loadNotifications();
    }

    // 加载通知
    async loadNotifications() {
        try {
            const response = await notificationAPI.getUserNotifications();
            if (response.success) {
                this.notifications = response.data.notifications || [];
                this.unreadCount = response.data.unreadCount || 0;
                this.displayNotifications();
                this.updateNotificationBadge();
            }
        } catch (error) {
            console.error('加载通知失败:', error);
        }
    }

    // 初始化通知UI
    initializeNotificationUI() {
        this.createNotificationPanel();
    }

    // 创建通知面板
    createNotificationPanel() {
        const panel = document.createElement('div');
        panel.id = 'notificationPanel';
        panel.className = 'notification-panel';
        panel.innerHTML = `
            <div class="notification-header">
                <h3>通知 <span id="unreadCountBadge" class="badge">0</span></h3>
                <div class="notification-actions">
                    <button onclick="markAllAsRead()" class="btn btn-sm">全部已读</button>
                    <button class="close-notifications" onclick="toggleNotificationPanel()">×</button>
                </div>
            </div>
            <div class="notification-list" id="notificationList">
                <!-- 通知列表将在这里动态生成 -->
            </div>
        `;

        document.body.appendChild(panel);
    }

    // 显示通知
    displayNotifications() {
        const notificationList = document.getElementById('notificationList');
        if (!notificationList) return;

        if (this.notifications.length === 0) {
            notificationList.innerHTML = '<div class="no-notifications">暂无通知</div>';
            return;
        }

        let html = '';
        this.notifications.forEach(notification => {
            html += this.renderNotification(notification);
        });

        notificationList.innerHTML = html;
    }

    // 渲染单个通知
    renderNotification(notification) {
        const isUnread = !notification.read;

        return `
            <div class="notification-item ${isUnread ? 'unread' : ''}" data-notification-id="${notification.id}">
                <div class="notification-content">
                    <div class="notification-title">${notification.title}</div>
                    <div class="notification-message">${notification.message}</div>
                    <div class="notification-time">${new Date(notification.createdAt).toLocaleString()}</div>
                </div>
                <div class="notification-actions">
                    ${isUnread ? `
                        <button onclick="markAsRead(${notification.id})" class="btn btn-sm">标记已读</button>
                    ` : ''}
                    <button onclick="deleteNotification(${notification.id})" class="btn btn-sm btn-delete">删除</button>
                </div>
            </div>
        `;
    }

    // 更新通知徽章
    updateNotificationBadge() {
        // 对面板中的通知徽章和通知按钮处的通知徽章都要进行修改
        const badges = document.querySelectorAll('#unreadCountBadge');

        badges.forEach((badge, index) => {
            badge.textContent = this.unreadCount;
            badge.style.display = this.unreadCount > 0 ? 'inline-block' : 'none';
        });

        // 更新页面标题
        if (this.unreadCount > 0) {
            document.title = `(${this.unreadCount}) 编辑文档 - 协作编辑系统`;
        } else {
            document.title = '编辑文档 - 协作编辑系统';
        }
    }

    // 处理新通知
    handleNewNotification(notification) {
        this.notifications.unshift(notification);
        if (!notification.read) {
            this.unreadCount++;
        }
        this.displayNotifications();
        this.updateNotificationBadge();
    }

    // 标记通知为已读
    async markAsRead(notificationId) {
        try {
            const response = await notificationAPI.markAsRead(notificationId);
            if (response.success) {
                // 更新本地状态
                const notification = this.notifications.find(n => n.id === notificationId);
                if (notification && !notification.read) {
                    notification.read = true;
                    this.unreadCount--;
                    this.displayNotifications();
                    this.updateNotificationBadge();
                }
            }
        } catch (error) {
            console.error('标记通知已读失败:', error);
        }
    }

    // 标记所有通知为已读
    async markAllAsRead() {
        try {
            const response = await notificationAPI.markAllAsRead();
            if (response.success) {
                // 更新所有通知状态
                this.notifications.forEach(notification => {
                    notification.read = true;
                });
                this.unreadCount = 0;
                this.displayNotifications();
                this.updateNotificationBadge();
            }
        } catch (error) {
            console.error('标记所有通知已读失败:', error);
        }
    }

    // 删除通知
    async deleteNotification(notificationId) {
        if (!confirm('确定要删除这条通知吗？')) return;

        try {
            const response = await notificationAPI.deleteNotification(notificationId);
            if (response.success) {
                // 从本地移除
                const notificationIndex = this.notifications.findIndex(n => n.id === notificationId);
                if (notificationIndex !== -1) {
                    const notification = this.notifications[notificationIndex];
                    if (!notification.read) {
                        this.unreadCount--;
                    }
                    this.notifications.splice(notificationIndex, 1);
                    this.displayNotifications();
                    this.updateNotificationBadge();
                }
            }
        } catch (error) {
            console.error('删除通知失败:', error);
        }
    }
}