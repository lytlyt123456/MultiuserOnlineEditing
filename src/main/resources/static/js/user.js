const user = {
    // 获取用户资料
    async getProfile() {
        try {
            const response = await apiRequest('/user/get-profile');
            return response;
        } catch (error) {
            console.error('获取用户资料错误:', error);
            throw error;
        }
    },

    // 更新用户资料
    async updateProfile(profileData) {
        try {
            const response = await apiRequest('/user/profile', {
                method: 'PUT',
                body: JSON.stringify(profileData)
            });
            return response;
        } catch (error) {
            console.error('更新用户资料错误:', error);
            throw error;
        }
    },

    // 根据用户名获取用户信息（管理员）
    async getProfileByUsername(username) {
        try {
            const response = await apiRequest(`/user/${username}/get-profile-by-username`);
            return response;
        } catch (error) {
            console.error('获取用户资料错误:', error);
            throw error;
        }
    },

    // 根据用户名获取用户信息（管理员）
    async getUserIdByUsername(username) {
        try {
            const response = await apiRequest(`/user/${username}/get-id-by-username`);
            return response;
        } catch (error) {
            console.error('获取用户资料错误:', error);
            throw error;
        }
    },

    // 根据用户ID获取用户信息（管理员）
    async getUserProfileById(userId) {
        try {
            const response = await apiRequest(`/user/${userId}/get-profile-by-id`);
            return response;
        } catch (error) {
            console.error('获取用户信息错误:', error);
            throw error;
        }
    },

    // 上传头像
    async uploadAvatar(file) {
        try {
            const formData = new FormData();
            formData.append('file', file);

            const response = await fetch(`${API_BASE_URL}/user/avatar`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${getToken()}`,
                },
                body: formData
            });

            const result = await response.json();
            return result;
        } catch (error) {
            console.error('上传头像错误:', error);
            throw error;
        }
    },

    async removeAvatar() {
        try {
            const response = await apiRequest('/user/remove-avatar');
            return response;
        } catch(error) {
            console.error('删除头像失败:', error);
            throw error;
        }
    },

    // 获取操作日志
    async getOperationLogs() {
        try {
            const response = await apiRequest('/user/logs');
            return response;
        } catch (error) {
            console.error('获取操作日志错误:', error);
            throw error;
        }
    },

    // 管理员获取指定用户的操作日志
    async getAllOperationLogs(userId) {
        try {
            const response = await apiRequest(`/user/${userId}/logs`);
            return response;
        } catch (error) {
            console.error('获取用户操作日志错误:', error);
            throw error;
        }
    },

    // 申请升级角色
    async requestRoleUpgrade() {
        try {
            const response = await apiRequest('/user/request-upgrade', {
                method: 'POST'
            });
            return response;
        } catch (error) {
            console.error('申请升级角色错误:', error);
            throw error;
        }
    },

    // 获取升级申请列表（管理员）
    async getUpgradeRequests() {
        try {
            const response = await apiRequest('/user/upgrade-requests');
            return response;
        } catch (error) {
            console.error('获取升级申请错误:', error);
            throw error;
        }
    },

    // 更改用户角色（管理员）
    async changeUserRole(userId, newRole) {
        try {
            const response = await apiRequest(`/user/${userId}/role`, {
                method: 'PUT',
                body: JSON.stringify({ role: newRole })
            });
            return response;
        } catch (error) {
            console.error('更改用户角色错误:', error);
            throw error;
        }
    },
};