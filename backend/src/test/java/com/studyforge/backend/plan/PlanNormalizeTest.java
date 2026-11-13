package com.studyforge.backend.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.studyforge.backend.common.ApiException;

class PlanNormalizeTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 16);

    @Test
    void recomputesWeekDatesAndPlacesAssessments() {
        List<PlanContent.Week> weeks = new ArrayList<>();
        weeks.add(new PlanContent.Week(7, "x", "y", " Introducere ", List.of("A", " "), List.of("T"), null));
        weeks.add(null);
        weeks.add(new PlanContent.Week(9, null, null, "Recapitulare", List.of("B"), List.of(), List.of()));
        weeks.add(new PlanContent.Week(10, null, null, "Test", List.of("C"), null, List.of("test 1")));

        PlanContent raw = new PlanContent(null, "Baze de date", "Rezumat", null, null,
                List.of(
                        new PlanContent.Assessment("Examen", "", "examen", List.of("Tot")),
                        new PlanContent.Assessment("Test 1", "2026-12-03", "test", List.of("SQL")),
                        new PlanContent.Assessment("Colocviu", "2026-11-19", "colocviu", null)),
                weeks, null);

        PlanContent result = PlanService.normalize(raw, MONDAY);

        assertThat(result.title()).isEqualTo("Plan de învățare – Baze de date");
        assertThat(result.startDate()).isEqualTo("2026-11-16");
        assertThat(result.endDate()).isEqualTo("2026-12-06");
        assertThat(result.weeks()).hasSize(3);
        assertThat(result.weeks().get(0).weekNumber()).isEqualTo(1);
        assertThat(result.weeks().get(0).startDate()).isEqualTo("2026-11-16");
        assertThat(result.weeks().get(0).focus()).isEqualTo("Introducere");
        assertThat(result.weeks().get(0).topics()).containsExactly("A");
        assertThat(result.weeks().get(0).assessments()).containsExactly("Colocviu");
        // "test 1" deja prezent (alt case) -> nu se dubleaza
        assertThat(result.weeks().get(2).assessments()).containsExactly("test 1");
        // evaluarile sortate dupa data, cele fara data la final
        assertThat(result.assessments()).extracting(PlanContent.Assessment::name)
                .containsExactly("Colocviu", "Test 1", "Examen");
        assertThat(result.tips()).isEmpty();
    }

    @Test
    void rejectsPlanWithoutWeeks() {
        PlanContent raw = new PlanContent("t", "s", null, null, null, null, List.of(), null);
        assertThatThrownBy(() -> PlanService.normalize(raw, MONDAY)).isInstanceOf(ApiException.class);
    }
}
