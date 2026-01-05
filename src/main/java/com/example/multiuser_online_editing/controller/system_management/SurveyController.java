package com.example.multiuser_online_editing.controller.system_management;

import com.example.multiuser_online_editing.entity.system_management.*;
import com.example.multiuser_online_editing.service.system_management.SurveyService;
import com.example.multiuser_online_editing.service.user_management.UserService;
import com.example.multiuser_online_editing.controller.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/surveys")
public class SurveyController {

    @Autowired
    private SurveyService surveyService;

    @Autowired
    private UserService userService;

    /**
     * 获取问卷问题列表
     */
    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<Object>> getSurveyQuestions() {
        try {
            List<SurveyQuestion> questions = SurveyQuestions.QUESTIONS;
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("questions", questions);
            responseData.put("totalQuestions", questions.size());
            responseData.put("likertQuestions", SurveyQuestions.getLikertQuestionNumber());
            responseData.put("textQuestions", SurveyQuestions.getTextQuestionNumber());

            return ResponseEntity.ok(ApiResponse.success("获取问卷问题成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 检查用户是否有资格填写问卷
     */
    @GetMapping("/eligibility")
    public ResponseEntity<ApiResponse<Object>> checkEligibility() {
        try {
            Long currentUserId = userService.getCurrentUserId();
            boolean isEligible = surveyService.isUserEligible(currentUserId);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("eligible", isEligible);

            return ResponseEntity.ok(ApiResponse.success("资格检查成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 提交问卷
     */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<Object>> submitSurvey(@RequestBody SubmitSurveyRequest request) {
        try {
            Long currentUserId = userService.getCurrentUserId();
            Long surveyId = surveyService.createOrUpdateSurvey(currentUserId, request.getAnswers());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("surveyId", surveyId);

            return ResponseEntity.ok(ApiResponse.success("问卷提交成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取用户自己的问卷
     */
    @GetMapping("/my-survey")
    public ResponseEntity<ApiResponse<Object>> getMySurvey() {
        try {
            Long currentUserId = userService.getCurrentUserId();
            Optional<Survey> optionalSurvey = surveyService.getUserSurvey(currentUserId);

            Survey survey;
            if (optionalSurvey.isPresent())
                survey = optionalSurvey.get();
            else survey = null;

            return ResponseEntity.ok(ApiResponse.success("获取问卷成功", survey));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 管理员：高级搜索问卷
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Object>> searchSurveys(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer scoreLevel) {
        try {
            List<?> surveys = surveyService.searchSurveys(userId, username, scoreLevel);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("surveys", surveys);
            responseData.put("count", surveys.size());

            return ResponseEntity.ok(ApiResponse.success("搜索成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 管理员：获取Likert问题评分分布
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/distribution/{questionIndex}")
    public ResponseEntity<ApiResponse<Object>> getScoreDistribution(@PathVariable Integer questionIndex) {
        try {
            Map<Integer, Long> distribution = surveyService.getScoreDistribution(questionIndex);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("questionIndex", questionIndex);
            responseData.put("distribution", distribution);

            // 计算总数
            Long total = distribution.values().stream().mapToLong(Long::longValue).sum();
            responseData.put("totalResponses", total);

            return ResponseEntity.ok(ApiResponse.success("获取评分分布成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 管理员：获取所有问卷
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Object>> getAllSurveys() {
        try {
            List<?> surveys = surveyService.getAllSurveys();

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("surveys", surveys);
            responseData.put("count", surveys.size());

            return ResponseEntity.ok(ApiResponse.success("获取所有问卷成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 请求DTO类
    static class SubmitSurveyRequest {
        private Map<Integer, Object> answers;

        public Map<Integer, Object> getAnswers() { return answers; }
        public void setAnswers(Map<Integer, Object> answers) { this.answers = answers; }
    }
}