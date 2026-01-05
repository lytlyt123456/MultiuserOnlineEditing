class TaskManager {
    constructor() {
        this.currentDocumentId = null;
        this.tasks = [];
    }

    // 初始化任务功能
    async initialize(documentId) {
        this.currentDocumentId = documentId;
        this.initializeTaskUI();
        await this.loadTasks();
    }

    // 加载任务
    async loadTasks() {
        try {
            const response = await taskAPI.getDocumentTasks(this.currentDocumentId);
            if (response.success) {
                this.tasks = response.data.tasks || [];
                this.displayTasks();
            }
        } catch (error) {
            console.error('加载任务失败:', error);
        }
    }

    // 初始化任务UI
    initializeTaskUI() {
        this.createTaskSidebar();
    }

    // 创建任务侧边栏
    createTaskSidebar() {
        const sidebar = document.createElement('div');
        sidebar.id = 'taskSidebar';
        sidebar.className = 'task-sidebar';
        sidebar.innerHTML = `
            <div class="task-header">
                <h3>任务</h3>
                <button class="close-tasks" onclick="toggleTaskSidebar()">×</button>
            </div>
            <div class="task-actions">
                <button onclick="showCreateTaskModal()" class="btn btn-primary">新建任务</button>
            </div>
            <div class="task-list" id="taskList">
                <!-- 任务列表将在这里动态生成 -->
            </div>
        `;

        document.body.appendChild(sidebar);
        this.createTaskModal();
    }

    // 切换任务侧边栏显示/隐藏
    toggleTaskSidebar() {
        const sidebar = document.getElementById('taskSidebar');
        if (sidebar) {
            sidebar.classList.toggle('active');
        }
    }

    // 显示创建任务模态框
    showCreateTaskModal() {
        const modal = document.getElementById('createTaskModal');
        if (modal) {
            modal.style.display = 'flex';
        }
    }

    // 关闭创建任务模态框
    closeCreateTaskModal() {
        const modal = document.getElementById('createTaskModal');
        if (modal) {
            modal.style.display = 'none';
            // 清空表单
            document.getElementById('taskTitle').value = '';
            document.getElementById('taskDescription').value = '';
            document.getElementById('taskPriority').value = 'MEDIUM';
            document.getElementById('taskDueDate').value = '';
        }
    }

    // 创建任务模态框
    createTaskModal() {
        const modal = document.createElement('div');
        modal.id = 'createTaskModal';
        modal.className = 'modal';
        modal.style.display = 'none';
        modal.innerHTML = `
            <div class="modal-content">
                <div class="modal-header">
                    <h3>创建新任务</h3>
                    <span class="close" onclick="closeCreateTaskModal()">&times;</span>
                </div>
                <div class="modal-body">
                    <div class="form-group">
                        <label for="taskTitle">任务标题 *</label>
                        <input type="text" id="taskTitle" placeholder="请输入任务标题">
                    </div>
                    <div class="form-group">
                        <label for="taskDescription">任务描述</label>
                        <textarea id="taskDescription" placeholder="请输入任务描述"></textarea>
                    </div>
                    <div class="form-group">
                        <label for="taskPriority">优先级</label>
                        <select id="taskPriority">
                            <option value="LOW">低</option>
                            <option value="MEDIUM" selected>中</option>
                            <option value="HIGH">高</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="taskDueDate">截止日期</label>
                        <input type="datetime-local" id="taskDueDate">
                    </div>
                    <div class="form-actions">
                        <button onclick="createNewTask()" class="btn btn-primary">创建</button>
                        <button onclick="closeCreateTaskModal()" class="btn btn-outline">取消</button>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(modal);
    }

    // 显示任务
    displayTasks() {
        const taskList = document.getElementById('taskList');
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

        return `
            <div class="task-item" data-task-id="${task.id}">
                <div class="task-header">
                    <div class="task-title">${task.title}</div>
                    <div class="task-meta">
                        <span class="task-status ${statusClass}">${this.getStatusText(task.status)}</span>
                        <span class="task-priority ${priorityClass}">${this.getPriorityText(task.priority)}</span>
                    </div>
                </div>
                ${task.description ? `<div class="task-description">${task.description}</div>` : ''}
                ${this.isTaskOverdue(task) ? '⚠️ 已过期 ' : ''}
                ${task.dueDate ? `<div class="task-due-date">截止: ${new Date(task.dueDate).toLocaleString()}</div>` : ''}
                <div class="task-actions">
                    ${this.renderStatusButtons(task)}
                </div>
            </div>
        `;
    }

    // 检查任务是否过期
    isTaskOverdue(task) {
        if (!task.dueDate || task.status === 'COMPLETED' || task.status === 'CANCELLED') {
            return false;
        }
        return new Date(task.dueDate) < new Date();
    }

    // 渲染状态按钮
    renderStatusButtons(task) {
        const buttons = [];

        if (this.isTaskOverdue(task)) {
            return '';
        }

        if (task.status === 'PENDING') {
            buttons.push(`<button onclick="updateTaskStatus(${task.id}, 'IN_PROGRESS')" class="btn btn-sm">开始</button>`);
        }

        if (task.status === 'IN_PROGRESS') {
            buttons.push(`<button onclick="updateTaskStatus(${task.id}, 'COMPLETED')" class="btn btn-sm">完成</button>`);
        }

        if (task.status !== 'CANCELLED') {
            buttons.push(`<button onclick="updateTaskStatus(${task.id}, 'CANCELLED')" class="btn btn-sm">取消</button>`);
        }

        return buttons.join('');
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

    // 处理任务更新
    handleTasksUpdate(tasks) {
        this.tasks = tasks;
        this.displayTasks();
    }

    // 创建新任务
    createNewTask() {
        const title = document.getElementById('taskTitle').value.trim();
        const description = document.getElementById('taskDescription').value.trim();
        const priority = document.getElementById('taskPriority').value;
        const dueDate = document.getElementById('taskDueDate').value;

        if (!title) {
            alert('请输入任务标题');
            return;
        }

        const taskData = {
            title: title,
            description: description,
            priority: priority
        };

        if (dueDate) {
            taskData.dueDate = dueDate;
        }

        this.createTask(taskData);
        this.closeCreateTaskModal();
    }

    // 创建任务
    async createTask(taskData) {
        try {
            const response = await taskAPI.createTask(this.currentDocumentId, taskData);
            if (response.success) {
                await this.loadTasks(); // 重新加载任务
            }
        } catch (error) {
            console.error('创建任务失败:', error);
            alert('创建任务失败: ' + error.message);
        }
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