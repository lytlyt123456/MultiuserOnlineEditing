package com.example.multiuser_online_editing.repository.system_management;

import com.example.multiuser_online_editing.entity.system_management.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, Long> {

    Optional<Survey> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("SELECT s FROM Survey s JOIN s.user u WHERE " +
            "(:userId IS NULL OR s.user.id = :userId) AND " +
            "(:username IS NULL OR u.username = :username) " +
            "ORDER BY s.createdAt DESC")
    List<Survey> findByUserIdOrUsername(
            @Param("userId") Long userId,
            @Param("username") String username);

    @Query("SELECT COUNT(sa) FROM SurveyAnswer sa WHERE " +
            "sa.questionIndex = :questionIndex AND " +
            "sa.likertAnswer = :score AND " +
            "sa.likertAnswer IS NOT NULL")
    Long countByQuestionAndScore(
            @Param("questionIndex") Integer questionIndex,
            @Param("score") Integer score);
}