package com.studyforge.backend.auth;

import java.time.Instant;
import java.util.UUID;

/** Corpurile JSON pentru /api/auth. */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** POST /api/auth/register */
    public record RegisterRequest(String username, String email, String gender, String password) {
    }

    /** POST /api/auth/login */
    public record LoginRequest(String email, String password) {
    }

    public record UserResponse(UUID id, String username, String email, Gender gender, Instant createdAt) {

        static UserResponse of(AppUser user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getGender(),
                    user.getCreatedAt());
        }
    }

    /**
     * Raspunsul la register/login. Frontend-ul trimite token-ul la fiecare cerere:
     * {@code Authorization: Bearer <token>}
     */
    public record AuthResponse(String token, String tokenType, Instant expiresAt, UserResponse user) {
    }
}
