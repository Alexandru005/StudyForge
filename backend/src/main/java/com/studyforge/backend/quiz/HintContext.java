package com.studyforge.backend.quiz;

/**
 * Datele necesare pentru generarea unui hint, citite dintr-o singura interogare.
 * subject poate fi null (setul nu e legat de un plan).
 */
public record HintContext(String question, String answer, String learningStage, String subject) {
}