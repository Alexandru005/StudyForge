package com.studyforge.backend.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    /** Emailul e salvat mereu cu litere mici, deci cautarea se face tot cu litere mici. */
    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
