package com.studyforge.backend.quiz;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Obiectele JSON returnate de /api/quizzes. */
public final class QuizDtos {

    private QuizDtos() {
    }

    public record QuizSummary(
            UUID id,
            UUID planId,
            String planTitle,
            Integer weekNumber,
            String learningStage,
            long flashcardCount,
            String sourceFileName,
            Instant createdAt,
            String pdfUrl,
            String downloadUrl) {

        static QuizSummary of(Quiz quiz, UUID planId, String planTitle, long flashcardCount) {
            String base = "/api/quizzes/" + quiz.getId() + "/pdf";
            return new QuizSummary(quiz.getId(), planId, planTitle, quiz.getWeekNumber(), quiz.getLearningStage(),
                    flashcardCount, quiz.getSourceFileName(), quiz.getCreatedAt(), base, base + "?download=true");
        }
    }

    public record FlashcardItem(UUID id, String question, String answer, boolean mastered) {

        static FlashcardItem of(Flashcard card) {
            return new FlashcardItem(card.getId(), card.getQuestion(), card.getAnswer(), card.isMastered());
        }
    }

    /** Setul + toate flashcard-urile (pentru afisare / parcurgere in UI). */
    public record QuizDetails(QuizSummary quiz, List<FlashcardItem> flashcards) {
    }
}
