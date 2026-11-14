package com.studyforge.backend.auth;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import com.studyforge.backend.common.ApiException;

/**
 * Scoate ID-ul utilizatorului din token-ul JWT ("sub").
 * In controllere: {@code @AuthenticationPrincipal Jwt jwt} -> {@code CurrentUser.id(jwt)}.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Trebuie să fii autentificat.");
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token invalid. Autentifică-te din nou.");
        }
    }
}
