class CommentManager {
    constructor() {
        this.currentDocumentId = null;
        this.currentUserId = null;
        this.comments = [];
    }

    // 初始化评论功能
    async initialize(documentId) {
        this.currentDocumentId = documentId;

        // 获取当前用户信息
        const userResponse = await user.getProfile();
        if (userResponse.success) {
            this.currentUserId = userResponse.data.userId;
        }

        // 初始化评论UI
        this.initializeCommentUI();

        // 加载评论
        await this.loadComments();
    }

    // 加载评论
    async loadComments() {
        try {
            const response = await commentAPI.getDocumentComments(this.currentDocumentId);
            if (response.success) {
                this.comments = response.data.comments || [];
                this.displayComments();
            }
        } catch (error) {
            console.error('加载评论失败:', error);
        }
    }

    // 初始化评论UI
    initializeCommentUI() {
        // 创建评论侧边栏
        this.createCommentSidebar();
    }

    // 创建评论侧边栏
    createCommentSidebar() {
        const sidebar = document.createElement('div');
        sidebar.id = 'commentSidebar';
        sidebar.className = 'comment-sidebar';
        sidebar.innerHTML = `
            <div class="comment-header">
                <h3>评论</h3>
                <button class="close-comments" onclick="toggleCommentSidebar()">×</button>
            </div>
            <div class="comment-list" id="commentList">
                <!-- 评论列表将在这里动态生成 -->
            </div>
            <div class="add-comment">
                <textarea id="newComment" placeholder="添加评论..."></textarea>
                <button onclick="addNewComment()" class="btn btn-primary">发送</button>
            </div>
        `;

        document.body.appendChild(sidebar);
    }

    // 切换评论侧边栏显示/隐藏
    toggleCommentSidebar() {
        const sidebar = document.getElementById('commentSidebar');
        if (sidebar) {
            sidebar.classList.toggle('active');
        }
    }

    // 显示评论
    displayComments() {
        const commentList = document.getElementById('commentList');
        if (!commentList) return;

        if (this.comments.length === 0) {
            commentList.innerHTML = '<div class="no-comments">暂无评论</div>';
            return;
        }

        let html = '';
        this.comments.forEach(comment => {
            if (!comment.parent) { // 只显示顶级评论
                html += this.renderComment(comment);
            }
        });

        commentList.innerHTML = html;
    }

    // 渲染单个评论
    renderComment(comment) {
        const isResolved = comment.resolved;
        const isAuthor = comment.user.id === this.currentUserId;

        const avatarFileName = this.getFileNameFromPath(comment.user.avatar_path);
        let avatarUrl = '';
        if (avatarFileName) {
            avatarUrl = `http://localhost:8080/uploads/avatars/${avatarFileName}`;
        }

        if (avatarUrl) {
        return `
            <div class="comment-item ${isResolved ? 'resolved' : ''}" data-comment-id="${comment.id}">
                <div class="comment-header">
                    <img src="${avatarUrl}" alt="${comment.user.username}">
                    <div class="comment-user">${comment.user.username}</div>
                    <div class="comment-time">${new Date(comment.createdAt).toLocaleString()}</div>
                </div>
                <div class="comment-content">${comment.content}</div>
                ${comment.position ? `<div class="comment-position">位置: ${comment.position || 'null'}</div>` : ''}
                <div class="comment-actions">
                    ${!isResolved ? `
                        <button onclick="resolveComment(${comment.id})" class="btn btn-sm">标记解决</button>
                    ` : `
                        <span class="resolved-badge">已解决</span>
                    `}
                    ${isAuthor ? `
                        <button onclick="deleteComment(${comment.id})" class="btn btn-sm btn-delete">删除</button>
                    ` : ''}
                    <button onclick="showReplyForm(${comment.id})" class="btn btn-sm">回复</button>
                </div>
                ${this.renderReplies(comment.id)}
            </div>
        `;
        } else {
        return `
            <div class="comment-item ${isResolved ? 'resolved' : ''}" data-comment-id="${comment.id}">
                <div class="comment-header">
                    <div class="avatar-placeholder">${comment.user.username.charAt(0).toUpperCase()}</div>
                    <div class="comment-user">${comment.user.username}</div>
                    <div class="comment-time">${new Date(comment.createdAt).toLocaleString()}</div>
                </div>
                <div class="comment-content">${comment.content}</div>
                ${comment.position ? `<div class="comment-position">位置: ${comment.position || 'null'}</div>` : ''}
                <div class="comment-actions">
                    ${!isResolved ? `
                        <button onclick="resolveComment(${comment.id})" class="btn btn-sm">标记解决</button>
                    ` : `
                        <span class="resolved-badge">已解决</span>
                    `}
                    ${isAuthor ? `
                        <button onclick="deleteComment(${comment.id})" class="btn btn-sm btn-delete">删除</button>
                    ` : ''}
                    <button onclick="showReplyForm(${comment.id})" class="btn btn-sm">回复</button>
                </div>
                ${this.renderReplies(comment.id)}
            </div>
        `;
        }
    }

    // 从文件路径中提取文件名
    getFileNameFromPath(path) {
        if (!path) return '';
        return path.split('/').pop().split('\\').pop();
    }

    // 渲染回复
    renderReplies(commentId) {
        const replies = this.comments.filter(comment => comment.parent && comment.parent.id === commentId);
        if (replies.length === 0) return '';

        let html = '<div class="comment-replies">';
        replies.forEach(reply => {
            html += this.renderComment(reply);
        });
        html += '</div>';
        return html;
    }

    // 处理评论更新
    handleCommentsUpdate(comments) {
        this.comments = comments;
        this.displayComments();
    }

    async addNewComment() {
        const commentInput = document.getElementById('newComment');
        const content = commentInput.value.trim();

        if (!content) {
            alert('请输入评论内容');
            return;
        }

        await this.addComment(content);
    }

    // 添加评论
    async addComment(content, position = null) {
        try {
            const response = await commentAPI.addComment(this.currentDocumentId, content, position);
            if (response.success) {
                document.getElementById('newComment').value = '';
                await this.loadComments(); // 重新加载评论
            }
        } catch (error) {
            console.error('添加评论失败:', error);
            alert('添加评论失败: ' + error.message);
        }
    }

    // 显示回复表单
    showReplyForm(commentId) {
        const commentItem = document.querySelector(`[data-comment-id="${commentId}"]`);
        let replyForm = commentItem.querySelector('.reply-form');

        if (!replyForm) {
            replyForm = document.createElement('div');
            replyForm.className = 'reply-form';
            replyForm.innerHTML = `
                <textarea placeholder="回复评论..."></textarea>
                <div class="reply-actions">
                    <button onclick="submitReply(${commentId}, this)" class="btn btn-sm btn-primary">发送</button>
                    <button onclick="cancelReply(this)" class="btn btn-sm btn-outline">取消</button>
                </div>
            `;
            commentItem.appendChild(replyForm);
        }

        replyForm.style.display = 'block';
    }

    // 提交回复
    async submitReply(commentId, button) {
        const replyForm = button.parentElement.parentElement;
        const textarea = replyForm.querySelector('textarea');
        const content = textarea.value.trim();

        if (!content) {
            alert('请输入回复内容');
            return;
        }

        await this.replyToComment(commentId, content);
        replyForm.style.display = 'none';
        textarea.value = '';
    }

    // 取消回复
    cancelReply(button) {
        const replyForm = button.parentElement.parentElement;
        replyForm.style.display = 'none';
        replyForm.querySelector('textarea').value = '';
    }

    // 回复评论
    async replyToComment(commentId, content) {
        try {
            const response = await commentAPI.replyToComment(commentId, content);
            if (response.success) {
                await this.loadComments(); // 重新加载评论
            }
        } catch (error) {
            console.error('回复评论失败:', error);
            alert('回复评论失败: ' + error.message);
        }
    }

    // 标记评论为已解决
    async resolveComment(commentId) {
        try {
            const response = await commentAPI.resolveComment(commentId);
            if (response.success) {
                await this.loadComments(); // 重新加载评论
            }
        } catch (error) {
            console.error('标记评论解决失败:', error);
            alert('标记评论解决失败: ' + error.message);
        }
    }

    // 删除评论
    async deleteComment(commentId) {
        if (!confirm('确定要删除这条评论吗？')) return;

        try {
            const response = await commentAPI.deleteComment(commentId);
            if (response.success) {
                await this.loadComments(); // 重新加载评论
            }
        } catch (error) {
            console.error('删除评论失败:', error);
            alert('删除评论失败: ' + error.message);
        }
    }
}