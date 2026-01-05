// API基础URL
// 自动检测当前访问的地址
const getBaseUrl = () => {
    if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
        return 'http://localhost:8080/api';
    } else {
        return `${window.location.protocol}//${window.location.host}/api`;
    }
};
// window.location.protocol -> 客户端的应用层协议
// window.location.host -> 客户端所看到的服务器的IP地址和端口号（客户端访问服务器时所使用的IP地址和端口号）

const API_BASE_URL = getBaseUrl();

// 显示消息
function showMessage(elementId, text, type = 'error') {
    const messageEl = document.getElementById(elementId);
    if (messageEl) {
        messageEl.textContent = text;
        messageEl.className = `message ${type}`;
        messageEl.style.display = 'block';

        // 3秒后自动隐藏
        if (type === 'success') {
            setTimeout(() => {
                messageEl.style.display = 'none';
            }, 3000);
        }
    }
}

// 获取认证token
function getToken() {
    return localStorage.getItem('token');
}

// 检查登录状态
function checkAuth() {
    const token = getToken();
    if (!token) {
        window.location.href = 'login.html';
        return false;
    }
    return true;
}

// 通用API请求
async function apiRequest(endpoint, options = {}) {
    const token = getToken();
    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    if (token) {
        defaultOptions.headers['Authorization'] = `Bearer ${token}`;
    }

    const config = { ...defaultOptions, ...options };

    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, config);
        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || '请求失败');
        }

        return data;
    } catch (error) {
        console.error('API请求错误:', error);
        throw error;
    }
}

// 登出
function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = 'login.html';
}