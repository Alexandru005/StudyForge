package com.studyforge.backend.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.studyforge.backend.plan.PlanContent;

/**
 * Genereaza PDF-urile si le citeste inapoi: verifica diacriticele, metadatele si paginarea.
 * Nu are nevoie de baza de date sau de Gemini.
 */
class PdfRenderingTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    @Test
    void planPdfContainsWeeksDiacriticsAndPlanId() {
        UUID planId = UUID.randomUUID();
        List<PlanContent.Week> weeks = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            weeks.add(new PlanContent.Week(i, "2026-11-" + String.format("%02d", 2 + (i - 1) % 4 * 7),
                    "2026-11-" + String.format("%02d", 8 + (i - 1) % 4 * 7),
                    "Înțelegerea noțiunilor de bază din capitolul " + i,
                    List.of("Funcții și șiruri", "Limite de funcții – definiție și proprietăți"),
                    List.of("Rezolvă 5 exerciții din culegere", "Fă o fișă cu formulele importante 📚"),
                    i == 4 ? List.of("Test 1") : List.of()));
        }
        PlanContent plan = new PlanContent("Plan de învățare – Analiză matematică", "Analiză matematică",
                "Planul acoperă toată programa și include recapitulări înaintea fiecărui test.",
                "2026-11-02", "2027-01-24",
                List.of(new PlanContent.Assessment("Test 1", "2026-11-27", "test", List.of("Capitolele 1–3"))),
                weeks, List.of("Învață puțin în fiecare zi."));

        byte[] pdf = new PlanPdfRenderer().render(planId, plan);

        assertThat(pdf.length).isGreaterThan(1000);
        PdfTextExtractor.Extracted extracted = extractor.extract(pdf);
        assertThat(extracted.planId()).isEqualTo(planId.toString());
        assertThat(extracted.docType()).isEqualTo("plan");
        assertThat(extracted.pageCount()).isGreaterThan(1);
        assertThat(extracted.text())
                .contains("Săptămâna 1")
                .contains("Săptămâna 12")
                .contains("Funcții și șiruri")
                .contains("Evaluare în această săptămână")
                .contains("Pagina 1 din");
    }

    @Test
    void flashcardPdfContainsQuestionsAndAnswers() {
        List<FlashcardPdfRenderer.Card> cards = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            cards.add(new FlashcardPdfRenderer.Card("Ce este limita unei funcții într-un punct? (" + i + ")",
                    "Valoarea de care se apropie f(x) când x tinde către acel punct. ".repeat(i % 5 + 1)));
        }
        UUID planId = UUID.randomUUID();

        byte[] pdf = new FlashcardPdfRenderer().render(planId, "Analiză matematică",
                "Săptămâna 3 · Limite", "16.11 – 22.11.2026", cards);

        PdfTextExtractor.Extracted extracted = extractor.extract(pdf);
        assertThat(extracted.docType()).isEqualTo("flashcards");
        assertThat(extracted.planId()).isEqualTo(planId.toString());
        assertThat(extracted.text())
                .contains("ÎNTREBAREA 1")
                .contains("ÎNTREBAREA 20")
                .contains("RĂSPUNS")
                .contains("tinde către");
    }

    @Test
    void veryLongCardDoesNotBreakLayout() {
        String longAnswer = "Explicație foarte lungă. ".repeat(400);
        byte[] pdf = new FlashcardPdfRenderer().render(null, null, "Săptămâna 1", "",
                List.of(new FlashcardPdfRenderer.Card("Întrebare?", longAnswer)));
        PdfTextExtractor.Extracted extracted = extractor.extract(pdf);
        assertThat(extracted.pageCount()).isGreaterThan(1);
        assertThat(extracted.planId()).isNull();
    }
}
