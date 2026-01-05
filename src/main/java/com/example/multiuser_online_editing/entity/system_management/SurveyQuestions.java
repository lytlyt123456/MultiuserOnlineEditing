package com.example.multiuser_online_editing.entity.system_management;

import java.util.ArrayList;
import java.util.List;

public class SurveyQuestions {

    public static final List<SurveyQuestion> QUESTIONS = new ArrayList<>();

    static {
        // 五点量表问题（Likert Scale 1-5）
        QUESTIONS.add(new SurveyQuestion(true, "您对该协作编辑系统的整体满意度如何？"));
        QUESTIONS.add(new SurveyQuestion(true, "系统界面的易用性如何？"));
        QUESTIONS.add(new SurveyQuestion(true, "实时协作功能的响应速度如何？"));
        QUESTIONS.add(new SurveyQuestion(true, "文档编辑功能的完善程度如何？"));
        QUESTIONS.add(new SurveyQuestion(true, "评论功能对协作是否有帮助？"));
        QUESTIONS.add(new SurveyQuestion(true, "任务功能是否实用？"));
        QUESTIONS.add(new SurveyQuestion(true, "视频会议功能的稳定性如何？"));
        QUESTIONS.add(new SurveyQuestion(true, "系统性能和加载速度如何？"));
        QUESTIONS.add(new SurveyQuestion(true, "您是否愿意向同事推荐该系统？"));
        QUESTIONS.add(new SurveyQuestion(true, "系统是否满足了您的协作编辑需求？"));

        // 开放性问题
        QUESTIONS.add(new SurveyQuestion(false, "您最喜欢该系统的哪个功能？为什么？"));
        QUESTIONS.add(new SurveyQuestion(false, "您认为系统最需要改进的地方是什么？"));
        QUESTIONS.add(new SurveyQuestion(false, "您在使用过程中遇到过哪些技术问题？"));
        QUESTIONS.add(new SurveyQuestion(false, "对于系统的未来版本，您有什么功能建议？"));
        QUESTIONS.add(new SurveyQuestion(false, "您觉得系统的用户界面设计有哪些可以优化的地方？"));
    }

    public static int getQuestionNumber() {
        return QUESTIONS.size();
    }

    public static int getLikertQuestionNumber() {
        return (int) QUESTIONS.stream()
                .filter(SurveyQuestion::getIsLikert)
                .count();
    }

    public static int getTextQuestionNumber() {
        return (int) QUESTIONS.stream()
                .filter(q -> !q.getIsLikert())
                .count();
    }
}