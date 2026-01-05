const documentAPI = {
    // 创建文档
    async createDocument(documentData) {
        try {
            const response = await apiRequest('/documents', {
                method: 'POST',
                body: JSON.stringify(documentData)
            });
            return response;
        } catch (error) {
            console.error('创建文档错误:', error);
            throw error;
        }
    },

    // 使用模板创建文档
    async createDocumentFromTemplate(templateData) {
        try {
            const response = await apiRequest('/documents/from-template', {
                method: 'POST',
                body: JSON.stringify(templateData)
            });
            return response;
        } catch (error) {
            console.error('使用模板创建文档错误:', error);
            throw error;
        }
    },

    // 更新文档
    async updateDocument(documentId, updateData) {
        try {
            const response = await apiRequest(`/documents/${documentId}`, {
                method: 'PUT',
                body: JSON.stringify(updateData)
            });
            return response;
        } catch (error) {
            console.error('更新文档错误:', error);
            throw error;
        }
    },

    // 自动保存文档
    async autoSaveDocument(documentId, content) {
        try {
            const response = await apiRequest(`/documents/${documentId}/auto-save`, {
                method: 'POST',
                body: JSON.stringify({ content })
            });
            return response;
        } catch (error) {
            console.error('自动保存文档错误:', error);
            throw error;
        }
    },

    // 恢复自动保存内容
    async restoreAutoSaveContent(documentId) {
        try {
            const response = await apiRequest(`/documents/${documentId}/restore-auto-save`);
            return response;
        } catch (error) {
            console.error('恢复自动保存内容错误:', error);
            throw error;
        }
    },

    // 高级搜索（作为所有者）
    async advancedSearchOwner(searchParams) {
        try {
            const queryParams = new URLSearchParams();
            if (searchParams.title) queryParams.append('title', searchParams.title);
            if (searchParams.content) queryParams.append('content', searchParams.content);
            if (searchParams.startDate) queryParams.append('startDate', searchParams.startDate);
            if (searchParams.endDate) queryParams.append('endDate', searchParams.endDate);
            if (searchParams.tagName) queryParams.append('tagName', searchParams.tagName);
            if (searchParams.page) queryParams.append('page', searchParams.page);
            if (searchParams.size) queryParams.append('size', searchParams.size);

            const response = await apiRequest(`/documents/advanced-search-owner?${queryParams}`);
            return response;
        } catch (error) {
            console.error('高级搜索错误:', error);
            throw error;
        }
    },

    // 高级搜索（作为协作者）
    async advancedSearchCollaborator(searchParams) {
        try {
            const queryParams = new URLSearchParams();
            if (searchParams.title) queryParams.append('title', searchParams.title);
            if (searchParams.content) queryParams.append('content', searchParams.content);
            if (searchParams.ownerUsername) queryParams.append('ownerUsername', searchParams.ownerUsername);
            if (searchParams.startDate) queryParams.append('startDate', searchParams.startDate);
            if (searchParams.endDate) queryParams.append('endDate', searchParams.endDate);
            if (searchParams.tagName) queryParams.append('tagName', searchParams.tagName);
            if (searchParams.page) queryParams.append('page', searchParams.page);
            if (searchParams.size) queryParams.append('size', searchParams.size);

            const response = await apiRequest(`/documents/advanced-search-collaborator?${queryParams}`);
            return response;
        } catch (error) {
            console.error('高级搜索错误:', error);
            throw error;
        }
    },

    // 添加协作者
    async addCollaborator(documentId, userId) {
        try {
            const response = await apiRequest(`/documents/${documentId}/collaborators`, {
                method: 'POST',
                body: JSON.stringify({ userId })
            });
            return response;
        } catch (error) {
            console.error('添加协作者错误:', error);
            throw error;
        }
    },

    // 删除文档（软删除）
    async deleteDocument(documentId) {
        try {
            const response = await apiRequest(`/documents/${documentId}`, {
                method: 'DELETE'
            });
            return response;
        } catch (error) {
            console.error('删除文档错误:', error);
            throw error;
        }
    },

    // 获取文档详情
    async getDocumentDetail(documentId) {
        try {
            const response = await apiRequest(`/documents/${documentId}`);
            return response;
        } catch (error) {
            console.error('获取文档详情错误:', error);
            throw error;
        }
    },

    // 移除协作者
    async removeCollaborator(documentId, userId) {
        try {
            const response = await apiRequest(`/documents/${documentId}/collaborators/${userId}`, {
                method: 'DELETE'
            });
            return response;
        } catch (error) {
            console.error('移除协作者错误:', error);
            throw error;
        }
    },

    // 获取根目录下的文档
    async getRootDocuments() {
        try {
            const response = await apiRequest('/documents/root-documents');
            return response;
        } catch (error) {
            console.error('获取根目录文档错误:', error);
            throw error;
        }
    },

    // 获取回收站中的文档
    async getRecycleBinDocuments() {
        try {
            const response = await apiRequest('/documents/recycle-bin');
            return response;
        } catch (error) {
            console.error('获取回收站文档错误:', error);
            throw error;
        }
    },

    // 永久删除文档
    async deleteDocumentForever(documentId) {
        try {
            const response = await apiRequest(`/documents/${documentId}/del-forever`, {
                method: 'DELETE'
            });
            return response;
        } catch (error) {
            console.error('永久删除文档错误:', error);
            throw error;
        }
    },

    // 恢复文档
    async restoreDocument(documentId) {
        try {
            const response = await apiRequest(`/documents/${documentId}/restore-document`);
            return response;
        } catch (error) {
            console.error('恢复文档错误:', error);
            throw error;
        }
    },

    // AI搜索文档
    async aiSearch(content) {
        try {
            const response = await apiRequest(`/documents/advanced-search-ai?content=${encodeURIComponent(content)}`);
            return response;
        } catch (error) {
            console.error('AI搜索错误:', error);
            throw error;
        }
    },

    // 文档聚类
    async documentClustering(k) {
        try {
            const response = await apiRequest(`/documents/document-clustering?k=${k}`);
            return response;
        } catch (error) {
            console.error('文档聚类错误:', error);
            throw error;
        }
    }
};

const folderAPI = {
    // 创建文件夹
    async createFolder(folderData) {
        try {
            const response = await apiRequest('/folders', {
                method: 'POST',
                body: JSON.stringify(folderData)
            });
            return response;
        } catch (error) {
            console.error('创建文件夹错误:', error);
            throw error;
        }
    },

    // 更新文件夹
    async updateFolder(folderId, updateData) {
        try {
            const response = await apiRequest(`/folders/${folderId}`, {
                method: 'PUT',
                body: JSON.stringify(updateData)
            });
            return response;
        } catch (error) {
            console.error('更新文件夹错误:', error);
            throw error;
        }
    },

    // 删除文件夹
    async deleteFolder(folderId) {
        try {
            const response = await apiRequest(`/folders/${folderId}`, {
                method: 'DELETE'
            });
            return response;
        } catch (error) {
            console.error('删除文件夹错误:', error);
            throw error;
        }
    },

    // 获取根文件夹列表
    async getRootFolders() {
        try {
            const response = await apiRequest('/folders/root');
            return response;
        } catch (error) {
            console.error('获取根文件夹列表错误:', error);
            throw error;
        }
    },

    // 获取子文件夹列表
    async getSubFolders(folderId) {
        try {
            const response = await apiRequest(`/folders/${folderId}/subfolders`);
            return response;
        } catch (error) {
            console.error('获取子文件夹列表错误:', error);
            throw error;
        }
    },

    // 获取子文件列表
    async getSubDocuments(folderId) {
        try {
            const response = await apiRequest(`/folders/${folderId}subdocuments`);
            return response;
        } catch (error) {
            console.error('获取子文件列表错误:', error);
            throw error;
        }
    },

    // 获取文件夹详情
    async getFolderDetail(folderId) {
        try {
            const response = await apiRequest(`/folders/${folderId}`);
            return response;
        } catch (error) {
            console.error('获取文件夹详情错误:', error);
            throw error;
        }
    },

    // 移动文件夹
    async moveFolder(folderId, newParentId) {
        try {
            const response = await apiRequest(`/folders/${folderId}/move`, {
                method: 'PUT',
                body: JSON.stringify({ newParentId })
            });
            return response;
        } catch (error) {
            console.error('移动文件夹错误:', error);
            throw error;
        }
    },
};

const tagAPI = {
    // 创建标签
    async createTag(tagData) {
        try {
            const response = await apiRequest('/tags', {
                method: 'POST',
                body: JSON.stringify(tagData)
            });
            return response;
        } catch (error) {
            console.error('创建标签错误:', error);
            throw error;
        }
    },

    // 更新标签
    async updateTag(tagId, updateData) {
        try {
            const response = await apiRequest(`/tags/${tagId}`, {
                method: 'PUT',
                body: JSON.stringify(updateData)
            });
            return response;
        } catch (error) {
            console.error('更新标签错误:', error);
            throw error;
        }
    },

    // 删除标签
    async deleteTag(tagId) {
        try {
            const response = await apiRequest(`/tags/${tagId}`, {
                method: 'DELETE'
            });
            return response;
        } catch (error) {
            console.error('删除标签错误:', error);
            throw error;
        }
    },

    // 获取用户的所有标签
    async getUserTags() {
        try {
            const response = await apiRequest('/tags');
            return response;
        } catch (error) {
            console.error('获取标签列表错误:', error);
            throw error;
        }
    },

    // 搜索标签
    async searchTags(keyword) {
        try {
            const response = await apiRequest(`/tags/search?keyword=${encodeURIComponent(keyword)}`);
            return response;
        } catch (error) {
            console.error('搜索标签错误:', error);
            throw error;
        }
    },

    // 获取标签详情
    async getTagDetail(tagId) {
        try {
            const response = await apiRequest(`/tags/${tagId}`);
            return response;
        } catch (error) {
            console.error('获取标签详情错误:', error);
            throw error;
        }
    },

    // 批量创建标签
    async batchCreateTags(tagNames) {
        try {
            const response = await apiRequest('/tags/batch', {
                method: 'POST',
                body: JSON.stringify({ tagNames })
            });
            return response;
        } catch (error) {
            console.error('批量创建标签错误:', error);
            throw error;
        }
    },
};

const templateAPI = {
    // 创建模板
    async createTemplate(templateData) {
        try {
            const response = await apiRequest('/templates', {
                method: 'POST',
                body: JSON.stringify(templateData)
            });
            return response;
        } catch (error) {
            console.error('创建模板错误:', error);
            throw error;
        }
    },

    // 更新模板
    async updateTemplate(templateId, updateData) {
        try {
            const response = await apiRequest(`/templates/${templateId}`, {
                method: 'PUT',
                body: JSON.stringify(updateData)
            });
            return response;
        } catch (error) {
            console.error('更新模板错误:', error);
            throw error;
        }
    },

    // 删除模板
    async deleteTemplate(templateId) {
        try {
            const response = await apiRequest(`/templates/${templateId}`, {
                method: 'DELETE'
            });
            return response;
        } catch (error) {
            console.error('删除模板错误:', error);
            throw error;
        }
    },

    // 获取可用模板列表（自己的模板 + 公开模板）
    async getAvailableTemplates(page = 0, size = 20) {
        try {
            const response = await apiRequest(`/templates?page=${page}&size=${size}`);
            return response;
        } catch (error) {
            console.error('获取模板列表错误:', error);
            throw error;
        }
    },

    // 获取用户的私有模板
    async getUserTemplates() {
        try {
            const response = await apiRequest('/templates/my-templates');
            return response;
        } catch (error) {
            console.error('获取用户模板列表错误:', error);
            throw error;
        }
    },

    // 按分类获取公开模板
    async getPublicTemplatesByCategory(category) {
        try {
            const response = await apiRequest(`/templates/public?category=${category}`);
            return response;
        } catch (error) {
            console.error('获取公开模板错误:', error);
            throw error;
        }
    },

    // 获取模板详情
    async getTemplateDetail(templateId) {
        try {
            const response = await apiRequest(`/templates/${templateId}`);
            return response;
        } catch (error) {
            console.error('获取模板详情错误:', error);
            throw error;
        }
    },

    // 切换模板公开状态
    async toggleTemplateVisibility(templateId) {
        try {
            const response = await apiRequest(`/templates/${templateId}/toggle-visibility`, {
                method: 'PUT'
            });
            return response;
        } catch (error) {
            console.error('切换模板公开状态错误:', error);
            throw error;
        }
    },
};