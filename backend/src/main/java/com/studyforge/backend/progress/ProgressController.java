package com.studyforge.backend.progress;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studyforge.backend.auth.CurrentUser;

/**
 * Sincronizarea progresului (tinut in browser) cu serverul, pentru clasamentul cu prietenii.
 */
@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressSyncService service;
    private final ProgressService documents;

    public ProgressController(ProgressSyncService service, ProgressService documents) {
        this.service = service;
        this.documents = documents;
    }

    /** Progresul complet salvat pe server: { "data": { v, subjects, days } | null, "updatedAt": ... }. */
    @GetMapping
    public ProgressService.ProgressResponse get(@AuthenticationPrincipal Jwt jwt) {
        return documents.get(CurrentUser.id(jwt));
    }

    /**
     * Salveaza progresul complet (acelasi JSON ca in localStorage) si actualizeaza totalurile pe zile
     * pentru clasament. Corp: { v: 1, subjects: {...}, days: {...} }.
     */
    @PutMapping
    public ProgressService.ProgressResponse save(@AuthenticationPrincipal Jwt jwt, @RequestBody ProgressDocument document) {
        return documents.save(CurrentUser.id(jwt), document);
    }

    /**
     * { "days": [ { "date": "2026-11-14", "correct": 12, "wrong": 3, "seconds": 1830.5 } ] }
     * Totalurile pe zi, adunate pe toate materiile (dayTotals din progress/stats.js: ok, bad, sec).
     */
    @PutMapping("/days")
    public ProgressSyncService.SyncResult sync(@AuthenticationPrincipal Jwt jwt,
            @RequestBody ProgressSyncService.SyncRequest request) {
        return service.sync(CurrentUser.id(jwt), request);
    }
}
