package com.mentorconnect.repository;

import com.mentorconnect.model.InterestTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InterestTagRepository extends JpaRepository<InterestTag, Long> {
    Optional<InterestTag> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
