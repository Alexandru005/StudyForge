package com.studyforge.backend.auth;

import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.studyforge.backend.config.AppProperties;

/**
 * Emite token-uri JWT semnate cu HS256.
 * "sub" = ID-ul utilizatorului; verificarea se face automat de Spring Security (vezi SecurityConfig).
 */
@Service
public class JwtService {

    static final String ISSUER = "studyforge";

    private final JwtEncoder encoder;
    private final AppProperties.Jwt settings;

    public JwtService(JwtEncoder encoder, AppProperties properties) {
        this.encoder = encoder;
        this.settings = properties.jwt();
    }

    public IssuedToken issue(AppUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(settings.expiration());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("email", user.getEmail())
                .claim("name", user.getUsername())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
