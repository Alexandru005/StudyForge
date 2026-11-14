package com.studyforge.backend.quiz;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studyforge.backend.auth.CurrentUser;

/**
 * POST /api/flashcards/{id}/hint
 * Corp optional: { "previousHint": "..." } (pentru butonul „Alt hint”)
 * Raspuns: { "hint": "..." }
 */
@RestController
@RequestMapping("/api/flashcards")
public class HintController {

    private final HintService service;

    public HintController(HintService service) {
        this.service = service;
    }

    public record HintRequest(String previousHint) {
    }

    public record HintResponse(String hint) {
    }

    @PostMapping("/{id}/hint")
    public HintResponse hint(@PathVariable("id") UUID id,
                             @RequestBody(required = false) HintRequest request,
                             @AuthenticationPrincipal Jwt jwt) {
        String previous = request != null ? request.previousHint() : null;
        return new HintResponse(service.generate(id, CurrentUser.id(jwt), previous));
    }
}