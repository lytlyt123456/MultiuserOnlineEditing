const auth = {
    // 用户注册
    async register(userData) {
        try {
            const response = await fetch(`${API_BASE_URL}/auth/signup`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(userData)
            });

            const result = await response.json();
            console.log('注册响应:', result); // 调试用

            // 适配新的响应格式
            if (result.success !== undefined) {
                return result; // 新格式: {success, message, data}
            } else {
                // 兼容旧格式
                return {
                    success: !!result.message && result.message.includes('成功'),
                    message: result.message
                };
            }
        } catch (error) {
            console.error('注册错误:', error);
            return {
                success: false,
                message: '网络错误，请重试'
            };
        }
    },

    // 用户登录
    async login(credentials) {
        try {
            const response = await fetch(`${API_BASE_URL}/auth/signin`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(credentials)
            });

            const result = await response.json();
            console.log('登录响应:', result); // 调试用

            // 适配新的响应格式
            if (result.success !== undefined) {
                return result;
            } else {
                // 兼容旧格式
                return {
                    success: !!result.token,
                    message: result.token ? '登录成功' : '登录失败',
                    data: result
                };
            }
        } catch (error) {
            console.error('登录错误:', error);
            return {
                success: false,
                message: '网络错误，请重试'
            };
        }
    },

    // 重置密码
    async resetPassword(email, newPassword) {
        try {
            const response = await fetch(`${API_BASE_URL}/auth/reset-password`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    email: email,
                    newPassword: newPassword
                })
            });

            const result = await response.json();
            console.log('密码重置响应:', result);

            // 适配响应格式
            if (result.success !== undefined) {
                return result;
            } else {
                return {
                    success: !!result.message && result.message.includes('成功'),
                    message: result.message
                };
            }
        } catch (error) {
            console.error('密码重置错误:', error);
            return {
                success: false,
                message: '网络错误，请重试'
            };
        }
    }
};