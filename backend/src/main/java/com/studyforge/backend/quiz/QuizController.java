package com.studyforge.backend.quiz;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.studyforge.backend.auth.CurrentUser;

/**
 * Pagina 3 (generare flashcard-uri) + listare/descarcare pentru pagina 4.
 */
@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService service;

    public QuizController(QuizService service) {
        this.service = service;
    }

    /**
     * multipart/form-data:
     * - file: PDF-ul planului generat pe pagina 2 (obligatoriu)
     * - week: saptamana in care e utilizatorul, de la 1 (obligatoriu)
     * - count: numarul de flashcard-uri, 5-40 (optional, implicit 15)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<QuizDtos.QuizDetails> create(
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "week", required = false) Integer week,
            @RequestParam(name = "count", required = false) Integer count,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(file, week, count, CurrentUser.id(jwt)));
    }

    /** ?planId=... pentru a filtra seturile unui plan. */
    @GetMapping
    public List<QuizDtos.QuizSummary> list(@RequestParam(name = "planId", required = false) UUID planId,
            @AuthenticationPrincipal Jwt jwt) {
        return service.list(planId, CurrentUser.id(jwt));
    }

    @GetMapping("/{id}")
    public QuizDtos.QuizDetails get(@PathVariable("id") UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.get(id, CurrentUser.id(jwt));
    }

    /** Pentru link-uri directe: ?access_token=TOKEN (vezi SecurityConfig). */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable("id") UUID id,
            @RequestParam(name = "download", defaultValue = "false") boolean download,
            @AuthenticationPrincipal Jwt jwt) {
        return service.pdf(id, CurrentUser.id(jwt)).toResponse(download);
    }
}
