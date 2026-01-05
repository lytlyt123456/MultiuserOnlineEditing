package com.example.multiuser_online_editing.repository.system_management;

import com.example.multiuser_online_editing.entity.system_management.SurveyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, Long> {

    List<SurveyAnswer> findBySurveyId(Long surveyId);

    @Query("SELECT sa FROM SurveyAnswer sa WHERE sa.survey.id = :surveyId AND sa.questionIndex = :questionIndex")
    Optional<SurveyAnswer> findBySurveyIdAndQuestionIndex(@Param("surveyId") Long surveyId,
                                                          @Param("questionIndex") Integer questionIndex);

    @Modifying
    @Query("UPDATE SurveyAnswer sa SET sa.likertAnswer = :likertAnswer " +
            "WHERE sa.survey.id = :surveyId AND sa.questionIndex = :questionIndex")
    void updateLikertAnswerBySurveyIdAndQuestionIndex(@Param("surveyId") Long surveyId,
                                                      @Param("questionIndex") Integer questionIndex,
                                                      @Param("likertAnswer") Integer likertAnswer);

    @Modifying
    @Query("UPDATE SurveyAnswer sa SET sa.textAnswer = :textAnswer " +
            "WHERE sa.survey.id = :surveyId AND sa.questionIndex = :questionIndex")
    void updateTextAnswerBySurveyIdAndQuestionIndex(@Param("surveyId") Long surveyId,
                                                    @Param("questionIndex") Integer questionIndex,
                                                    @Param("textAnswer") String textAnswer);
}