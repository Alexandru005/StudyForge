package com.studyforge.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Creare cont, autentificare si datele utilizatorului curent.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    /** { "username": "Popescu Ion", "email": "ion@exemplu.ro", "gender": "MALE", "password": "..." } */
    @PostMapping("/register")
    public ResponseEntity<AuthDtos.AuthResponse> register(@RequestBody AuthDtos.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }

    /** { "email": "ion@exemplu.ro", "password": "..." } */
    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@RequestBody AuthDtos.LoginRequest request) {
        return service.login(request);
    }

    /** Necesita token. Util la pornirea frontend-ului, ca sa verifice daca token-ul salvat mai e valid. */
    @GetMapping("/me")
    public AuthDtos.UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.me(CurrentUser.id(jwt));
    }
}
