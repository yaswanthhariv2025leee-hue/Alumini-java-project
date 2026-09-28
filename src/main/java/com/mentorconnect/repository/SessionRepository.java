package com.mentorconnect.repository;

import com.mentorconnect.model.Session;
import com.mentorconnect.model.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByMentorshipPairId(Long mentorshipPairId);
    List<Session> findByStatus(SessionStatus status);
    Page<Session> findByMentorshipPairId(Long mentorshipPairId, Pageable pageable);
    Page<Session> findByStatus(SessionStatus status, Pageable pageable);
    Page<Session> findByMentorshipPairIdAndStatus(Long mentorshipPairId, SessionStatus status, Pageable pageable);
    long countByMentorshipPairIdAndStatus(Long mentorshipPairId, SessionStatus status);
    long countByMentorshipPairId(Long mentorshipPairId);
    long countByStatus(SessionStatus status);

    @Query("SELECT COALESCE(SUM(s.durationMinutes), 0) FROM Session s WHERE s.status = :status")
    Long sumDurationMinutesByStatus(@Param("status") SessionStatus status);

    @Query("SELECT COALESCE(SUM(s.durationMinutes), 0) FROM Session s WHERE s.mentorshipPair.id = :pairId AND s.status = :status")
    Long sumDurationMinutesByPairIdAndStatus(@Param("pairId") Long pairId, @Param("status") SessionStatus status);
}
