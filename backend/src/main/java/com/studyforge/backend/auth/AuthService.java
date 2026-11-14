package com.studyforge.backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studyforge.backend.common.ApiException;

/**
 * Inregistrare si autentificare.
 * Emailul NU se verifica prin mesaj de confirmare: se valideaza doar formatul si unicitatea.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** Fiecare cuvant din nume: litere (inclusiv diacritice), optional legat cu "-" sau "'" (ex. Ana-Maria). */
    private static final Pattern NAME_WORD = Pattern.compile("\\p{L}+(?:['’-]\\p{L}+)*");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    static final int MIN_PASSWORD_LENGTH = 8;
    /** BCrypt foloseste doar primii 72 de octeti din parola. */
    static final int MAX_PASSWORD_BYTES = 72;

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** Hash folosit cand emailul nu exista, ca raspunsul sa dureze la fel (nu dezvaluie ce emailuri exista). */
    private final String dummyHash;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Completează numele, emailul și parola.");
        }
        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());
        Gender gender = Gender.parse(request.gender());
        validatePassword(request.password());

        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Există deja un cont cu acest email.");
        }
        AppUser user = new AppUser(username, email, gender, passwordEncoder.encode(request.password()));
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Doua inregistrari simultane cu acelasi email: constrangerea UNIQUE din DB o prinde.
            throw new ApiException(HttpStatus.CONFLICT, "Există deja un cont cu acest email.");
        }
        log.info("Cont nou: {}", user.getId());
        return respond(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        if (request == null || isBlank(request.email()) || isBlank(request.password())) {
            throw ApiException.badRequest("Completează emailul și parola.");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        AppUser user = users.findByEmail(email).orElse(null);
        String hash = user != null ? user.getPasswordHash() : dummyHash;
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user == null || !matches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email sau parolă greșită.");
        }
        return respond(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserResponse me(UUID userId) {
        return users.findById(userId)
                .map(AuthDtos.UserResponse::of)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "Contul nu mai există. Autentifică-te din nou."));
    }

    private AuthDtos.AuthResponse respond(AppUser user) {
        JwtService.IssuedToken token = jwtService.issue(user);
        return new AuthDtos.AuthResponse(token.value(), "Bearer", token.expiresAt(), AuthDtos.UserResponse.of(user));
    }

    // ------------------------------------------------------------------ validari (statice, testabile)

    /** "  popescu   ion " -> "popescu ion" (spatii curatate). Cere minim doua cuvinte: nume si prenume. */
    static String normalizeUsername(String raw) {
        if (isBlank(raw)) {
            throw ApiException.badRequest("Completează numele și prenumele.");
        }
        String name = raw.trim().replaceAll("\\s+", " ");
        if (name.length() > 100) {
            throw ApiException.badRequest("Numele poate avea maximum 100 de caractere.");
        }
        String[] words = name.split(" ");
        if (words.length < 2) {
            throw ApiException.badRequest("Scrie numele și prenumele (de exemplu „Popescu Ion”).");
        }
        for (String word : words) {
            if (!NAME_WORD.matcher(word).matches()) {
                throw ApiException.badRequest("Numele poate conține doar litere, spații și cratimă.");
            }
        }
        return name;
    }

    static String normalizeEmail(String raw) {
        if (isBlank(raw)) {
            throw ApiException.badRequest("Completează adresa de email.");
        }
        String email = raw.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 255 || !EMAIL.matcher(email).matches()) {
            throw ApiException.badRequest("Adresa de email nu are un format valid.");
        }
        return email;
    }

    static void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw ApiException.badRequest("Parola trebuie să aibă minimum " + MIN_PASSWORD_LENGTH + " caractere.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw ApiException.badRequest("Parola este prea lungă (maximum " + MAX_PASSWORD_BYTES + " de caractere).");
        }
        if (password.isBlank()) {
            throw ApiException.badRequest("Parola nu poate conține doar spații.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
