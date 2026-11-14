package com.studyforge.backend.joke;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studyforge.backend.auth.CurrentUser;

/**
 * POST /api/jokes
 * Corp optional: { "planId": "...", "previousJoke": "..." }
 * Raspuns: { "joke": "...", "subject": "Programare Orientată pe Obiecte", "planId": "..." }
 */
@RestController
@RequestMapping("/api/jokes")
public class JokeController {

    private final JokeService service;

    public JokeController(JokeService service) {
        this.service = service;
    }

    @PostMapping
    public JokeService.JokeResponse joke(@AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) JokeService.JokeRequest request) {
        return service.generate(CurrentUser.id(jwt), request);
    }
}
