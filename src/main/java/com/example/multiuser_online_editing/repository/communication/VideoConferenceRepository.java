package com.example.multiuser_online_editing.repository.communication;

import com.example.multiuser_online_editing.entity.communication.VideoConference;
import com.example.multiuser_online_editing.entity.communication.ConferenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoConferenceRepository extends JpaRepository<VideoConference, Long> {

    Optional<VideoConference> findByConferenceId(String conferenceId);

    List<VideoConference> findByDocumentIdAndStatusNot(Long documentId, ConferenceStatus status);

    List<VideoConference> findByCreatedByIdAndStatus(Long userId, ConferenceStatus status);

    @Query("SELECT vc FROM VideoConference vc JOIN vc.participants p WHERE p.user.id = :userId AND vc.status = :status")
    List<VideoConference> findParticipatingConferences(@Param("userId") Long userId, @Param("status") ConferenceStatus status);

    @Query("SELECT COUNT(p) FROM ConferenceParticipant p WHERE p.conference.id = :conferenceId AND p.status = 'JOINED'")
    Long countActiveParticipants(@Param("conferenceId") Long conferenceId);

    boolean existsByConferenceId(String conferenceId);
}