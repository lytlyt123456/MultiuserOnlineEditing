package com.example.multiuser_online_editing.service.system_management;

import com.example.multiuser_online_editing.entity.system_management.*;
import com.example.multiuser_online_editing.entity.user_management.*;
import com.example.multiuser_online_editing.repository.system_management.*;
import com.example.multiuser_online_editing.repository.user_management.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class SurveyService {

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveyAnswerRepository surveyAnswerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    /**
     * 检查用户是否有资格填写问卷
     */
    public boolean isUserEligible(Long userId) {
        // 检查用户角色是否为编辑者
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty() || user.get().getRole() != Role.EDITOR) {
            return false;
        }

        // 检查操作日志数量是否不少于100条
        Long operationLogCount = operationLogRepository.countByUserId(userId);
        return operationLogCount != null && operationLogCount >= 100;
    }

    /**
     * 创建或更新问卷
     */
    public Long createOrUpdateSurvey(Long userId, Map<Integer, Object> answers) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查用户资格
        if (!isUserEligible(userId)) {
            throw new RuntimeException("您没有资格填写问卷。要求：编辑者角色且操作日志超过100条。");
        }

        // 检查是否已填写问卷
        Optional<Survey> existingSurvey = surveyRepository.findByUserId(userId);
        Survey survey;

        if (existingSurvey.isPresent()) {
            // 更新现有问卷
            survey = existingSurvey.get();
            survey.setUpdatedAt(LocalDateTime.now());

            for (Map.Entry<Integer, Object> entry : answers.entrySet()) {
                Integer questionIndex = entry.getKey();
                Object answer = entry.getValue();

                // 验证问题编号
                if (questionIndex < 0 || questionIndex >= SurveyQuestions.getQuestionNumber()) {
                    throw new RuntimeException("无效的问题编号: " + questionIndex);
                }

                SurveyQuestion question = SurveyQuestions.QUESTIONS.get(questionIndex);

                if (question.getIsLikert()) {
                    surveyAnswerRepository.updateLikertAnswerBySurveyIdAndQuestionIndex(
                            survey.getId(), questionIndex, (Integer) answer
                    );
                } else {
                    surveyAnswerRepository.updateTextAnswerBySurveyIdAndQuestionIndex(
                            survey.getId(), questionIndex, (String) answer
                    );
                }
            }

            return survey.getId();
        }

        // 创建新问卷
        survey = new Survey();
        survey.setUser(user);

        // 保存问卷
        Survey savedSurvey = surveyRepository.save(survey);

        // 保存答案
        List<SurveyAnswer> surveyAnswers = new ArrayList<>();
        for (Map.Entry<Integer, Object> entry : answers.entrySet()) {
            Integer questionIndex = entry.getKey();
            Object answer = entry.getValue();

            // 验证问题编号
            if (questionIndex < 0 || questionIndex >= SurveyQuestions.getQuestionNumber()) {
                throw new RuntimeException("无效的问题编号: " + questionIndex);
            }

            SurveyQuestion question = SurveyQuestions.QUESTIONS.get(questionIndex);
            SurveyAnswer surveyAnswer = new SurveyAnswer();
            surveyAnswer.setSurvey(savedSurvey);
            surveyAnswer.setQuestionIndex(questionIndex);

            if (question.getIsLikert()) {
                // Likert问题
                if (!(answer instanceof Integer)) {
                    throw new RuntimeException("问题 " + questionIndex + " 需要整数答案（1-5）");
                }
                Integer likertAnswer = (Integer) answer;
                if (likertAnswer < 1 || likertAnswer > 5) {
                    throw new RuntimeException("Likert答案必须在1-5之间");
                }
                surveyAnswer.setLikertAnswer(likertAnswer);
            } else {
                // 文字问题
                if (!(answer instanceof String)) {
                    throw new RuntimeException("问题 " + questionIndex + " 需要文字答案");
                }
                String textAnswer = ((String) answer).trim();
                if (textAnswer.isEmpty()) {
                    throw new RuntimeException("问题 " + questionIndex + " 的答案不能为空");
                }
                surveyAnswer.setTextAnswer(textAnswer);
            }

            surveyAnswers.add(surveyAnswer);
        }

        // 验证答案数量
        if (surveyAnswers.size() != SurveyQuestions.getQuestionNumber()) {
            throw new RuntimeException("需要回答所有问题。问题总数: " + SurveyQuestions.getQuestionNumber());
        }

        surveyAnswerRepository.saveAll(surveyAnswers);
        savedSurvey.setAnswers(surveyAnswers);

        return savedSurvey.getId();
    }

    /**
     * 获取用户的问卷
     */
    public Optional<Survey> getUserSurvey(Long userId) {
        return surveyRepository.findByUserId(userId);
    }

    /**
     * 高级搜索问卷
     */
    public List<Survey> searchSurveys(Long userId, String username, Integer scoreLevel) {
        // 首先根据用户条件搜索
        List<Survey> surveys = surveyRepository.findByUserIdOrUsername(userId, username);

        // 然后根据平均分档次过滤
        if (scoreLevel != null && (scoreLevel < 1 || scoreLevel > 4)) {
            throw new RuntimeException("分数档次必须在1-4之间");
        }

        if (scoreLevel != null) {
            double minScore, maxScore;
            switch (scoreLevel) {
                case 1 -> { minScore = 1.0; maxScore = 2.0; }
                case 2 -> { minScore = 2.0; maxScore = 3.0; }
                case 3 -> { minScore = 3.0; maxScore = 4.0; }
                case 4 -> { minScore = 4.0; maxScore = 5.0; }
                default -> { minScore = 0.0; maxScore = 5.0; }
            }

            final double finalMinScore = minScore;
            final double finalMaxScore = maxScore;

            if (finalMinScore == 4.0 && finalMaxScore == 5.0) { // 左右都是闭区间
                surveys = surveys.stream()
                        .filter(survey -> {
                            Double avgScore = survey.getAverageLikertScore();
                            return avgScore >= finalMinScore && avgScore <= finalMaxScore;
                        })
                        .toList();
            } else { // 左闭右开
                surveys = surveys.stream()
                        .filter(survey -> {
                            Double avgScore = survey.getAverageLikertScore();
                            return avgScore >= finalMinScore && avgScore < finalMaxScore;
                        })
                        .toList();
            }
        }

        return surveys;
    }

    /**
     * 获取Likert问题的评分分布
     */
    public Map<Integer, Long> getScoreDistribution(Integer questionIndex) {
        // 验证问题索引
        if (questionIndex < 0 || questionIndex >= SurveyQuestions.getQuestionNumber()) {
            throw new RuntimeException("无效的问题编号: " + questionIndex);
        }

        SurveyQuestion question = SurveyQuestions.QUESTIONS.get(questionIndex);
        if (!question.getIsLikert()) {
            throw new RuntimeException("问题 " + questionIndex + " 不是Likert问题");
        }

        Map<Integer, Long> distribution = new HashMap<>();
        for (int score = 1; score <= 5; score++) {
            Long count = surveyRepository.countByQuestionAndScore(questionIndex, score);
            distribution.put(score, count != null ? count : 0L);
        }

        return distribution;
    }

    /**
     * 获取所有问卷
     */
    public List<Survey> getAllSurveys() {
        return surveyRepository.findAll();
    }

    /**
     * 检查用户是否已填写问卷
     */
    public boolean hasUserCompletedSurvey(Long userId) {
        return surveyRepository.existsByUserId(userId);
    }
}