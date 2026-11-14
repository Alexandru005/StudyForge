package com.studyforge.backend.config;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Setarile aplicatiei, citite din application.yaml (prefix "studyforge").
 */
@ConfigurationProperties(prefix = "studyforge")
public record AppProperties(
        String zone,
        Integer maxInputChars,
        Cors cors,
        Gemini gemini,
        Jwt jwt) {

    public AppProperties {
        if (zone == null || zone.isBlank()) {
            zone = "Europe/Bucharest";
        }
        if (maxInputChars == null || maxInputChars <= 0) {
            maxInputChars = 150_000;
        }
        if (cors == null) {
            cors = new Cors(List.of("http://localhost:5173"));
        }
        if (gemini == null) {
            gemini = new Gemini(null, null, null, null, null);
        }
        if (jwt == null) {
            jwt = new Jwt(null, null);
        }
    }

    public ZoneId zoneId() {
        return ZoneId.of(zone);
    }

    public record Cors(List<String> allowedOrigins) {
        public Cors {
            if (allowedOrigins == null) {
                allowedOrigins = List.of();
            }
        }
    }

    public record Gemini(String apiKey, String model, String baseUrl, Duration timeout, Integer maxRetries) {
        public Gemini {
            if (model == null || model.isBlank()) {
                model = "gemini-3.5-flash";
            }
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "https://generativelanguage.googleapis.com/v1beta";
            }
            if (timeout == null) {
                timeout = Duration.ofSeconds(180);
            }
            if (maxRetries == null || maxRetries < 0) {
                maxRetries = 2;
            }
        }

        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    /**
     * @param secret     cheia secreta pentru semnarea token-urilor (JWT_SECRET). Daca lipseste, se genereaza
     *                   una aleatorie la pornire si utilizatorii trebuie sa se autentifice din nou dupa restart.
     * @param expiration cat timp e valid un token (implicit 7 zile)
     */
    public record Jwt(String secret, Duration expiration) {
        public Jwt {
            if (expiration == null || expiration.isNegative() || expiration.isZero()) {
                expiration = Duration.ofDays(7);
            }
        }

        public boolean hasSecret() {
            return secret != null && !secret.isBlank();
        }
    }
}
