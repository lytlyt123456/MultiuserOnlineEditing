package com.example.multiuser_online_editing.repository.communication;

import com.example.multiuser_online_editing.entity.communication.ConferenceMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConferenceMessageRepository extends JpaRepository<ConferenceMessage, Long> {

    List<ConferenceMessage> findByConferenceIdOrderBySentAtAsc(Long conferenceId);

    @Query("SELECT cm FROM ConferenceMessage cm WHERE cm.conference.conferenceId = :conferenceId ORDER BY cm.sentAt ASC")
    List<ConferenceMessage> findByConferenceIdOrderBySentAtAsc(@Param("conferenceId") String conferenceId);

    @Query("SELECT cm FROM ConferenceMessage cm WHERE cm.conference.id = :conferenceId AND cm.sentAt > :since ORDER BY cm.sentAt ASC")
    List<ConferenceMessage> findMessagesSince(@Param("conferenceId") Long conferenceId, @Param("since") java.time.LocalDateTime since);
}