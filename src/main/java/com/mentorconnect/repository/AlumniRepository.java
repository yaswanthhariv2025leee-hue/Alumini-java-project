package com.mentorconnect.repository;

import com.mentorconnect.model.Alumni;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlumniRepository extends JpaRepository<Alumni, Long> {
    Optional<Alumni> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT DISTINCT a FROM Alumni a LEFT JOIN FETCH a.expertiseTags")
    List<Alumni> findAllWithTags();

    @Query("SELECT a FROM Alumni a LEFT JOIN FETCH a.expertiseTags WHERE a.id = :id")
    Optional<Alumni> findByIdWithTags(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Alumni a WHERE a.id = :id")
    Optional<Alumni> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT COUNT(a) FROM Alumni a JOIN a.expertiseTags t WHERE t.id = :tagId")
    long countAlumniWithTag(@Param("tagId") Long tagId);
}
