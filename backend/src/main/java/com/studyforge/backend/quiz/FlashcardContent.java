package com.studyforge.backend.quiz;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Raspunsul Gemini pentru flashcard-uri. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FlashcardContent(String weekLabel, List<Card> flashcards) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Card(String question, String answer) {
    }
}
