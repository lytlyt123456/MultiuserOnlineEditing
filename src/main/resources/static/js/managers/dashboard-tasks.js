class DashboardTaskManager {
    constructor() {
        this.tasks = [];
        this.currentFilter = 'all'; // 'all' 或 'pending'
        this.currentUserId = null;
    }

    // 初始化任务功能
    async initialize() {
        // 获取当前用户信息
        const userResponse = await user.getProfile();
        if (userResponse.success) {
            this.currentUserId = userResponse.data.userId;
        }

        // 初始化任务UI
        this.initializeTaskUI();

        // 设置WebSocket订阅
        this.setupWebSocketSubscriptions();

        // 加载任务
        await this.loadTasks();
    }

    // 初始化任务UI
    initializeTaskUI() {
        this.createTaskPanel();
    }

    // 创建任务面板
    createTaskPanel() {
        const panel = document.createElement('div');
        panel.id = 'dashboardTaskPanel';
        panel.className = 'dashboard-task-panel';
        panel.innerHTML = `
            <div class="task-panel-header">
                <h3>我的任务</h3>
                <div class="task-filter">
                    <button class="filter-btn ${this.currentFilter === 'all' ? 'active' : ''}" onclick="switchTaskFilter('all')">全部任务</button>
                    <button class="filter-btn ${this.currentFilter === 'pending' ? 'active' : ''}" onclick="switchTaskFilter('pending')">待处理</button>
                </div>
                <button class="close-tasks" onclick="toggleDashboardTaskPanel()">×</button>
            </div>
            <div class="task-panel-content">
                <div class="task-stats" id="taskStats">
                    <!-- 任务统计信息 -->
                </div>
                <div class="task-list" id="dashboardTaskList">
                    <!-- 任务列表将在这里动态生成 -->
                </div>
            </div>
        `;

        document.body.appendChild(panel);
    }

    // 设置WebSocket订阅
    setupWebSocketSubscriptions() {
        // 订阅任务更新（所有相关文档的任务更新）
        collaborationSocket.subscribe(`/topic/user/${this.currentUserId}/queue/task-updates`, (message) => {
            this.handleTaskUpdate();
        });
    }

    // 处理任务更新
    handleTaskUpdate() {
        // 重新加载任务以获取最新状态
        this.loadTasks();
    }

    // 加载任务
    async loadTasks() {
        try {
            let response;
            if (this.currentFilter === 'pending') {
                response = await taskAPI.getPendingTasks();
            } else {
                response = await taskAPI.getUserTasks();
            }

            if (response.success) {
                this.tasks = response.data.tasks || [];
                this.displayTasks();
                this.updateTaskStats();
            }
        } catch (error) {
            console.error('加载任务失败:', error);
        }
    }

    // 切换任务筛选
    async switchTaskFilter(filter) {
        this.currentFilter = filter;

        // 更新按钮状态
        document.querySelectorAll('.filter-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        event.target.classList.add('active');

        await this.loadTasks();
    }

    // 显示任务
    displayTasks() {
        const taskList = document.getElementById('dashboardTaskList');
        if (!taskList) return;

        if (this.tasks.length === 0) {
            taskList.innerHTML = '<div class="no-tasks">暂无任务</div>';
            return;
        }

        let html = '';
        this.tasks.forEach(task => {
            html += this.renderTask(task);
        });

        taskList.innerHTML = html;
    }

    // 渲染单个任务
    renderTask(task) {
        const statusClass = this.getStatusClass(task.status);
        const priorityClass = this.getPriorityClass(task.priority);
        const isOverdue = this.isTaskOverdue(task);

        return `
            <div class="dashboard-task-item ${isOverdue ? 'overdue' : ''}" data-task-id="${task.id}">
                <div class="task-main-info">
                    <div class="task-title">${task.title}</div>
                    <div class="task-document">文档: ${task.document.title}</div>
                </div>
                <div class="task-meta-info">
                    <div class="task-status-priority">
                        <span class="task-status ${statusClass}">${this.getStatusText(task.status)}</span>
                        <span class="task-priority ${priorityClass}">${this.getPriorityText(task.priority)}</span>
                    </div>
                    ${task.dueDate ? `
                        <div class="task-due-date ${isOverdue ? 'overdue' : ''}">
                            ${isOverdue ? '⚠️ 已过期 ' : ''}${new Date(task.dueDate).toLocaleString()}
                        </div>
                    ` : ''}
                </div>
                <div class="task-actions">
                    ${this.renderStatusButtons(task)}
                    <button onclick="openDocument('${task.document.id}')" class="btn btn-sm btn-outline">查看文档</button>
                </div>
            </div>
        `;
    }

    // 渲染状态按钮
    renderStatusButtons(task) {
        const buttons = [];

        if (this.isTaskOverdue(task)) {
            return '';
        }

        if (task.status === 'PENDING') {
            buttons.push(`<button onclick="updateDashboardTaskStatus(${task.id}, 'IN_PROGRESS')" class="btn btn-sm">开始</button>`);
        }

        if (task.status === 'IN_PROGRESS') {
            buttons.push(`<button onclick="updateDashboardTaskStatus(${task.id}, 'COMPLETED')" class="btn btn-sm">完成</button>`);
        }

        if (task.status !== 'CANCELLED') {
            buttons.push(`<button onclick="updateDashboardTaskStatus(${task.id}, 'CANCELLED')" class="btn btn-sm">取消</button>`);
        }

        return buttons.join('');
    }

    // 检查任务是否过期
    isTaskOverdue(task) {
        if (!task.dueDate || task.status === 'COMPLETED' || task.status === 'CANCELLED') {
            return false;
        }
        return new Date(task.dueDate) < new Date();
    }

    // 更新任务统计
    updateTaskStats() {
        const statsElement = document.getElementById('taskStats');
        if (!statsElement) return;

        const totalTasks = this.tasks.length;
        const pendingTasks = this.tasks.filter(task => task.status === 'PENDING').length;
        const inProgressTasks = this.tasks.filter(task => task.status === 'IN_PROGRESS').length;
        const overdueTasks = this.tasks.filter(task => this.isTaskOverdue(task)).length;

        statsElement.innerHTML = `
            <div class="stat-item">
                <span class="stat-number">${totalTasks}</span>
                <span class="stat-label">总任务</span>
            </div>
            <div class="stat-item">
                <span class="stat-number">${pendingTasks}</span>
                <span class="stat-label">待处理</span>
            </div>
            <div class="stat-item">
                <span class="stat-number">${inProgressTasks}</span>
                <span class="stat-label">进行中</span>
            </div>
            <div class="stat-item ${overdueTasks > 0 ? 'overdue' : ''}">
                <span class="stat-number">${overdueTasks}</span>
                <span class="stat-label">已过期</span>
            </div>
        `;
    }

    // 获取状态类名
    getStatusClass(status) {
        const classMap = {
            'PENDING': 'status-pending',
            'IN_PROGRESS': 'status-in-progress',
            'COMPLETED': 'status-completed',
            'CANCELLED': 'status-cancelled'
        };
        return classMap[status] || '';
    }

    // 获取优先级类名
    getPriorityClass(priority) {
        const classMap = {
            'LOW': 'priority-low',
            'MEDIUM': 'priority-medium',
            'HIGH': 'priority-high'
        };
        return classMap[priority] || '';
    }

    // 获取状态文本
    getStatusText(status) {
        const textMap = {
            'PENDING': '待处理',
            'IN_PROGRESS': '进行中',
            'COMPLETED': '已完成',
            'CANCELLED': '已取消'
        };
        return textMap[status] || status;
    }

    // 获取优先级文本
    getPriorityText(priority) {
        const textMap = {
            'LOW': '低',
            'MEDIUM': '中',
            'HIGH': '高'
        };
        return textMap[priority] || priority;
    }

    // 更新任务状态
    async updateTaskStatus(taskId, status) {
        try {
            const response = await taskAPI.updateTaskStatus(taskId, status);
            if (response.success) {
                await this.loadTasks(); // 重新加载任务
            }
        } catch (error) {
            console.error('更新任务状态失败:', error);
            alert('更新任务状态失败: ' + error.message);
        }
    }
}

// 全局函数
function toggleDashboardTaskPanel() {
    const panel = document.getElementById('dashboardTaskPanel');
    if (panel) {
        panel.classList.toggle('active');
    }
}

function switchTaskFilter(filter) {
    if (window.dashboardTaskManager) {
        window.dashboardTaskManager.switchTaskFilter(filter);
    }
}

function updateDashboardTaskStatus(taskId, status) {
    if (window.dashboardTaskManager) {
        window.dashboardTaskManager.updateTaskStatus(taskId, status);
    }
}

function openDocument(documentId) {
    window.open(`edit-document.html?id=${documentId}`, '_blank');
}