package com.studyforge.backend.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.studyforge.backend.common.ApiException;
import com.studyforge.backend.progress.ProgressDocument.Bucket;
import com.studyforge.backend.progress.ProgressDocument.Subject;
import com.studyforge.backend.progress.ProgressDocument.WeekCount;

/** Progresul complet salvat pe server: validare si totalurile pe zile pentru clasament. */
class ProgressDocumentTest {

    private static final LocalDate SAT = LocalDate.of(2026, 11, 14);

    @Test
    void documentIsCleanedAndSummedPerDay() {
        Map<String, Bucket> day = Map.of(
                "plan:x", new Bucket(600.44, 5, 1, Map.of("2", new WeekCount(5, 1), "abc", new WeekCount(1, 1))),
                "file:y", new Bucket(null, 2, null, null));
        ProgressDocument doc = new ProgressDocument(1, Map.of("plan:x", new Subject("Algoritmi")),
                Map.of("2026-11-13", day, "2026-12-30", Map.of("plan:x", new Bucket(1.0, 1, 1, null))));

        ProgressDocument clean = ProgressService.normalize(doc, SAT);

        assertThat(clean.days()).containsOnlyKeys("2026-11-13"); // ziua din viitor e ignorata
        assertThat(clean.subjects()).containsKeys("plan:x", "file:y");
        assertThat(clean.days().get("2026-11-13").get("plan:x").weeks()).containsOnlyKeys("2");
        var totals = ProgressService.dayTotals(clean);
        assertThat(totals).hasSize(1);
        assertThat(totals.get(0).correct()).isEqualTo(7);
        assertThat(totals.get(0).wrong()).isEqualTo(1);
        assertThat(totals.get(0).seconds()).isEqualTo(600);
    }

    @Test
    void invalidDocumentsAreRejected() {
        assertThatThrownBy(() -> ProgressService.normalize(null, SAT)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> ProgressService.normalize(new ProgressDocument(2, null, null), SAT))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> ProgressService.normalize(
                new ProgressDocument(1, null, Map.of("14.11.2026", Map.of())), SAT)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> ProgressService.normalize(
                new ProgressDocument(1, null, Map.of("2026-11-13", Map.of("k", new Bucket(-1.0, 0, 0, null)))), SAT))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> ProgressService.normalize(
                new ProgressDocument(1, null, Map.of("2026-11-13", Map.of("k", new Bucket(0.0, -3, 0, null)))), SAT))
                .isInstanceOf(ApiException.class);
    }
}
