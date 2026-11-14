package com.studyforge.backend.joke;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.studyforge.backend.plan.PlanContent;
import com.studyforge.backend.progress.WeekStats;

/** Promptul glumei folosește materia, săptămâna curentă, evaluările care urmează și scorul. */
class JokePromptTest {

    @Test
    void promptUsesPlanAndScore() {
        PlanContent content = new PlanContent("Plan POO", "Programare Orientată pe Obiecte", null, "2026-11-09", "2026-11-29",
                List.of(new PlanContent.Assessment("Test parțial", "2026-11-27", "Test", List.of()),
                        new PlanContent.Assessment("Lucrare veche", "2026-10-01", "Test", List.of())),
                List.of(new PlanContent.Week(2, "2026-11-09", "2026-11-15", "Moștenire", List.of("extends", "super"),
                        List.of(), List.of())),
                List.of());

        String prompt = JokeService.prompt("Programare Orientată pe Obiecte", content, LocalDate.of(2026, 11, 14),
                WeekStats.of(8, 2, 600), "Gluma veche");

        assertThat(prompt).contains("Programare Orientată pe Obiecte", "Moștenire", "extends", "Test parțial pe 2026-11-27",
                "8 din 10", "Gluma veche");
        assertThat(prompt).doesNotContain("Lucrare veche"); // evaluarea trecută nu apare
    }

    @Test
    void promptWithoutPlanContentStillWorks() {
        String prompt = JokeService.prompt("Algoritmi", null, LocalDate.of(2026, 11, 14), WeekStats.EMPTY, null);
        assertThat(prompt).contains("Algoritmi", "nu a rezolvat niciun flashcard");
    }
}
