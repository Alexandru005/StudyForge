package com.studyforge.backend.auth;

import java.time.Instant;

import com.studyforge.backend.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Un cont de utilizator.
 * - username: numele si prenumele (nu e unic)
 * - email: unic, salvat cu litere mici
 * - gender: MALE / FEMALE / OTHER
 * - passwordHash: parola hash-uita cu BCrypt (parola in clar nu se salveaza niciodata)
 */
@Entity
@Table(name = "app_user")
public class AppUser extends BaseEntity {

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10, columnDefinition = "varchar(10)")
    private Gender gender;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    protected AppUser() {
    }

    public AppUser(String username, String email, Gender gender, String passwordHash) {
        this.username = username;
        this.email = email;
        this.gender = gender;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Gender getGender() {
        return gender;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
