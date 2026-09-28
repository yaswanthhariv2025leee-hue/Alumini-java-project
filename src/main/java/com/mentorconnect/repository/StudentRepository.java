package com.mentorconnect.repository;

import com.mentorconnect.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT DISTINCT s FROM Student s LEFT JOIN FETCH s.interestTags")
    List<Student> findAllWithTags();

    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.interestTags WHERE s.id = :id")
    Optional<Student> findByIdWithTags(@Param("id") Long id);

    @Query("SELECT COUNT(s) FROM Student s JOIN s.interestTags t WHERE t.id = :tagId")
    long countStudentsWithTag(@Param("tagId") Long tagId);
}
