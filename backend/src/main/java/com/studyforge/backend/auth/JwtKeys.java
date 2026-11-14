package com.studyforge.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.studyforge.backend.config.AppProperties;

/**
 * Construieste cheia HMAC-SHA256 folosita la semnarea si verificarea token-urilor JWT.
 */
public final class JwtKeys {

    private static final Logger log = LoggerFactory.getLogger(JwtKeys.class);
    static final int MIN_SECRET_LENGTH = 32;

    private JwtKeys() {
    }

    public static SecretKey secretKey(AppProperties.Jwt jwt) {
        byte[] material;
        if (jwt.hasSecret()) {
            if (jwt.secret().length() < MIN_SECRET_LENGTH) {
                log.warn("JWT_SECRET are doar {} caractere. Foloseste minim {} caractere aleatorii.",
                        jwt.secret().length(), MIN_SECRET_LENGTH);
            }
            material = jwt.secret().getBytes(StandardCharsets.UTF_8);
        } else {
            log.warn("JWT_SECRET nu este setat: folosesc o cheie aleatorie. "
                    + "Dupa fiecare restart al backend-ului, utilizatorii trebuie sa se autentifice din nou.");
            material = new byte[64];
            new SecureRandom().nextBytes(material);
        }
        // SHA-256 da mereu exact 32 de octeti (256 biti), cat cere HS256, indiferent de lungimea secretului.
        return new SecretKeySpec(sha256(material), "HmacSHA256");
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponibil", e);
        }
    }
}
