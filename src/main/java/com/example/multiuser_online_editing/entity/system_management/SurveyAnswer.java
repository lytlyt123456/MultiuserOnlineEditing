package com.example.multiuser_online_editing.entity.system_management;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "survey_answers")
public class SurveyAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    @JsonIgnoreProperties({"answers", "user"})
    private Survey survey;

    @Column(name = "question_index", nullable = false)
    private Integer questionIndex; // 问题编号（第几个问题）

    @Column(name = "likert_answer")
    private Integer likertAnswer; // 五点量表答案（1-5），如果是Likert问题

    @Column(name = "text_answer", columnDefinition = "TEXT")
    private String textAnswer; // 文字答案，如果是开放性问题

    // 检查是否为Likert问题
    public boolean isLikertQuestion() {
        return SurveyQuestions.QUESTIONS.get(questionIndex).getIsLikert();
    }

    // 检查是否为文字问题
    public boolean isTextQuestion() {
        return !SurveyQuestions.QUESTIONS.get(questionIndex).getIsLikert();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Survey getSurvey() { return survey; }
    public void setSurvey(Survey survey) { this.survey = survey; }
    public Integer getQuestionIndex() { return questionIndex; }
    public void setQuestionIndex(Integer questionIndex) { this.questionIndex = questionIndex; }
    public Integer getLikertAnswer() { return likertAnswer; }
    public void setLikertAnswer(Integer likertAnswer) { this.likertAnswer = likertAnswer; }
    public String getTextAnswer() { return textAnswer; }
    public void setTextAnswer(String textAnswer) { this.textAnswer = textAnswer; }
}