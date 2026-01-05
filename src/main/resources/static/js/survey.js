const surveyAPI = {
    // 获取问卷问题列表
    async getSurveyQuestions() {
        try {
            const response = await apiRequest('/surveys/questions');
            return response;
        } catch (error) {
            console.error('获取问卷问题错误:', error);
            throw error;
        }
    },

    // 检查用户是否有资格填写问卷
    async checkEligibility() {
        try {
            const response = await apiRequest('/surveys/eligibility');
            return response;
        } catch (error) {
            console.error('检查资格错误:', error);
            throw error;
        }
    },

    // 提交问卷
    async submitSurvey(answers) {
        try {
            const response = await apiRequest('/surveys/submit', {
                method: 'POST',
                body: JSON.stringify({ answers })
            });
            return response;
        } catch (error) {
            console.error('提交问卷错误:', error);
            throw error;
        }
    },

    // 获取用户自己的问卷
    async getMySurvey() {
        try {
            const response = await apiRequest('/surveys/my-survey');
            return response;
        } catch (error) {
            console.error('获取用户问卷错误:', error);
            throw error;
        }
    },

    // 管理员：高级搜索问卷
    async searchSurveys(userId, username, scoreLevel) {
        try {
            const queryParams = new URLSearchParams();
            if (userId) queryParams.append('userId', userId);
            if (username) queryParams.append('username', username);
            if (scoreLevel) queryParams.append('scoreLevel', scoreLevel);

            const response = await apiRequest(`/surveys/search?${queryParams}`);
            return response;
        } catch (error) {
            console.error('搜索问卷错误:', error);
            throw error;
        }
    },

    // 管理员：获取Likert问题评分分布
    async getScoreDistribution(questionIndex) {
        try {
            const response = await apiRequest(`/surveys/distribution/${questionIndex}`);
            return response;
        } catch (error) {
            console.error('获取评分分布错误:', error);
            throw error;
        }
    },

    // 管理员：获取所有问卷
    async getAllSurveys() {
        try {
            const response = await apiRequest('/surveys/all');
            return response;
        } catch (error) {
            console.error('获取所有问卷错误:', error);
            throw error;
        }
    }
};