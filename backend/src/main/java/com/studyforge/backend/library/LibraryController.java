package com.studyforge.backend.library;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studyforge.backend.ai.GeminiClient;
import com.studyforge.backend.auth.CurrentUser;
import com.studyforge.backend.plan.PlanDtos;
import com.studyforge.backend.plan.PlanService;
import com.studyforge.backend.quiz.QuizDtos;
import com.studyforge.backend.quiz.QuizService;

/**
 * Pagina 4 (toate planurile + toate seturile de flashcard-uri) si un endpoint de status.
 */
@RestController
@RequestMapping("/api")
public class LibraryController {

    private final PlanService plans;
    private final QuizService quizzes;
    private final GeminiClient gemini;

    public LibraryController(PlanService plans, QuizService quizzes, GeminiClient gemini) {
        this.plans = plans;
        this.quizzes = quizzes;
        this.gemini = gemini;
    }

    public record Library(List<PlanDtos.PlanSummary> plans, List<QuizDtos.QuizSummary> quizzes) {
    }

    public record Status(String status, boolean geminiConfigured, String geminiModel) {
    }

    /** Planurile si seturile utilizatorului autentificat. */
    @GetMapping("/library")
    public Library library(@AuthenticationPrincipal Jwt jwt) {
        var userId = CurrentUser.id(jwt);
        return new Library(plans.list(userId), quizzes.list(null, userId));
    }

    /** Frontend-ul poate verifica la pornire daca cheia Gemini e setata. */
    @GetMapping("/status")
    public Status status() {
        return new Status("ok", gemini.isConfigured(), gemini.model());
    }
}
