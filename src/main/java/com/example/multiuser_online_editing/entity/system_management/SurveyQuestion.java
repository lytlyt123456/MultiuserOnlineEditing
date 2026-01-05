package com.example.multiuser_online_editing.entity.system_management;

public class SurveyQuestion {
    private boolean isLikert; // true就是五点问题，false就是输入文字的问题
    private String content; // 问题内容

    public SurveyQuestion(boolean isLikert, String content) {
        this.isLikert = isLikert;
        this.content = content;
    }

    public boolean getIsLikert() { return isLikert; }
    public String getContent() { return content; }
}