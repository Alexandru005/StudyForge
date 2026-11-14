package com.studyforge.backend.progress;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserProgressRepository extends JpaRepository<UserProgress, UUID> {

    @Query("select p from UserProgress p where p.user.id = :userId")
    Optional<UserProgress> findByUser(@Param("userId") UUID userId);
}
