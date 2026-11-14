package com.studyforge.backend.quiz;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.studyforge.backend.ai.GeminiClient;
import com.studyforge.backend.ai.GeminiSchemas;
import com.studyforge.backend.ai.Prompts;
import com.studyforge.backend.common.ApiException;

/**
 * Genereaza un indiciu pentru un flashcard. Hint-ul NU se salveaza in baza de date.
 */
@Service
public class HintService {

    private final FlashcardRepository flashcards;
    private final GeminiClient gemini;

    public HintService(FlashcardRepository flashcards, GeminiClient gemini) {
        this.flashcards = flashcards;
        this.gemini = gemini;
    }

    /**
     * @param previousHint hint-ul afisat anterior (optional), ca Gemini sa dea unul diferit la „Alt hint”
     */
    public String generate(UUID flashcardId, UUID userId, String previousHint) {
        HintContext context = flashcards.findHintContext(flashcardId, userId)
                .orElseThrow(() -> ApiException.notFound("Flashcard-ul nu a fost găsit."));

        HintResponse response = gemini.generateJson(Prompts.SYSTEM, Prompts.hint(context, previousHint),
                GeminiSchemas.hint(), HintResponse.class);

        if (response == null || response.hint() == null || response.hint().isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Nu am putut genera un hint. Încearcă din nou.");
        }
        return response.hint().trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HintResponse(String hint) {
    }
}