package com.studyforge.backend.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.studyforge.backend.auth.JwtKeys;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Securitatea aplicatiei:
 * - publice: /api/auth/register, /api/auth/login, /api/status
 * - restul /api/** cere header-ul {@code Authorization: Bearer <token>}
 * - pentru link-uri directe catre PDF (ex. {@code <a href>}), token-ul poate fi trimis si ca
 *   {@code ?access_token=<token>} la cererile GET
 * - fara sesiuni (stateless), fara CSRF (nu folosim cookie-uri)
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/status").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(unauthorizedEntryPoint()))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint()));
        return http.build();
    }

    @Bean
    SecretKey jwtSecretKey(AppProperties properties) {
        return JwtKeys.secretKey(properties.jwt());
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        // Verifica semnatura (HS256) si expirarea ("exp")
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        resolver.setAllowUriQueryParameter(true); // ?access_token=... (doar GET), pentru link-urile catre PDF
        return resolver;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /** 401 ca JSON (acelasi format ca restul erorilor), cu mesaj in romana. */
    private static AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            boolean hadToken = request.getHeader(HttpHeaders.AUTHORIZATION) != null
                    || request.getParameter("access_token") != null;
            String detail = hadToken
                    ? "Sesiunea a expirat sau token-ul este invalid. Autentifică-te din nou."
                    : "Trebuie să fii autentificat.";
            writeProblem(response, detail);
        };
    }

    private static void writeProblem(HttpServletResponse response, String detail) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/problem+json");
        response.getWriter().write("{\"status\":401,\"title\":\"Unauthorized\",\"detail\":\"" + detail + "\"}");
    }
}
