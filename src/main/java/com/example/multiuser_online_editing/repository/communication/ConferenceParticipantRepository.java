package com.example.multiuser_online_editing.repository.communication;

import com.example.multiuser_online_editing.entity.communication.ConferenceParticipant;
import com.example.multiuser_online_editing.entity.communication.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConferenceParticipantRepository extends JpaRepository<ConferenceParticipant, Long> {

    Optional<ConferenceParticipant> findByConferenceIdAndUserId(Long conferenceId, Long userId);

    List<ConferenceParticipant> findByConferenceIdAndStatus(Long conferenceId, ParticipantStatus status);

    @Query("SELECT cp FROM ConferenceParticipant cp WHERE cp.conference.conferenceId = :conferenceId AND cp.user.id = :userId")
    Optional<ConferenceParticipant> findByConferenceIdAndUserId(@Param("conferenceId") String conferenceId, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ConferenceParticipant cp SET cp.status = :status, cp.leftAt = CURRENT_TIMESTAMP WHERE cp.conference.id = :conferenceId AND cp.user.id = :userId")
    void updateParticipantStatus(@Param("conferenceId") Long conferenceId, @Param("userId") Long userId, @Param("status") ParticipantStatus status);

    @Query("SELECT COUNT(cp) FROM ConferenceParticipant cp WHERE cp.conference.id = :conferenceId AND cp.status = 'JOINED'")
    Long countJoinedParticipants(@Param("conferenceId") Long conferenceId);

    boolean existsByConferenceIdAndUserIdAndStatus(Long conferenceId, Long userId, ParticipantStatus status);

    List<ConferenceParticipant> findByUserIdAndStatus(Long userId, ParticipantStatus status);
}