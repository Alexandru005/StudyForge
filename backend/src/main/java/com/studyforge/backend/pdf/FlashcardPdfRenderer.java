package com.studyforge.backend.pdf;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Genereaza PDF-ul cu flashcard-uri (intrebare + raspuns).
 */
@Component
public class FlashcardPdfRenderer {

    public record Card(String question, String answer) {
    }

    /**
     * @param planId    planul din care provin (poate fi null)
     * @param subject   materia (poate fi null)
     * @param weekLabel ex. "Săptămâna 3 · Integrale definite"
     * @param dateRange ex. "17.11 – 23.11.2026" (poate fi gol)
     */
    public byte[] render(UUID planId, String subject, String weekLabel, String dateRange, List<Card> cards) {
        String title = "Flashcard-uri" + (subject != null && !subject.isBlank() ? " – " + subject : "");
        try (PdfLayout pdf = new PdfLayout("StudyForge · " + title + " · " + weekLabel)) {
            pdf.title(title);
            String subtitle = weekLabel
                    + (dateRange != null && !dateRange.isBlank() ? "  ·  " + dateRange : "")
                    + "  ·  " + cards.size() + (cards.size() == 1 ? " card" : " carduri");
            pdf.subtitle(subtitle);
            pdf.paragraph("Citește întrebarea, încearcă să răspunzi fără să te uiți, apoi verifică răspunsul.");
            pdf.spacer(4f);

            int index = 1;
            for (Card card : cards) {
                pdf.card(index++, card.question(), card.answer());
            }

            Map<String, String> metadata = new HashMap<>();
            metadata.put(PdfMetadata.DOC_TYPE, "flashcards");
            if (planId != null) {
                metadata.put(PdfMetadata.PLAN_ID, planId.toString());
            }
            return pdf.finish(title + " – " + weekLabel, metadata);
        } catch (IOException e) {
            throw new UncheckedIOException("Nu am putut genera PDF-ul cu flashcard-uri", e);
        }
    }
}
