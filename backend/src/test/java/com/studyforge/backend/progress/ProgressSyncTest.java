package com.studyforge.backend.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.studyforge.backend.common.ApiException;
import com.studyforge.backend.progress.ProgressSyncService.DayInput;
import com.studyforge.backend.progress.ProgressSyncService.SyncRequest;

/** Saptamanile, procentajul si validarea zilelor trimise de frontend. */
class ProgressSyncTest {

    private static final LocalDate SAT = LocalDate.of(2026, 11, 14); // sambata

    @Test
    void weeksRunMondayToSunday() {
        assertThat(Weeks.start(SAT, 0)).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(Weeks.start(SAT, -1)).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(Weeks.start(LocalDate.of(2026, 11, 9), 0)).isEqualTo(LocalDate.of(2026, 11, 9)); // luni
        assertThat(Weeks.start(LocalDate.of(2026, 11, 15), 0)).isEqualTo(LocalDate.of(2026, 11, 9)); // duminica
        assertThat(Weeks.offsetOf(SAT, LocalDate.of(2026, 11, 3))).isEqualTo(-1);

        assertThatThrownBy(() -> Weeks.start(SAT, 1)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Weeks.start(SAT, -Weeks.MAX_WEEKS_BACK - 1)).isInstanceOf(ApiException.class);
    }

    @Test
    void percentMatchesFrontendRounding() {
        assertThat(WeekStats.percent(0, 0)).isNull();
        assertThat(WeekStats.percent(42, 9)).isEqualTo(82);
        assertThat(WeekStats.percent(2, 1)).isEqualTo(67);
        assertThat(WeekStats.percent(1, 7)).isEqualTo(13); // 12.5 -> 13, ca Math.round din JS
        assertThat(WeekStats.of(42, 9, 11100).answered()).isEqualTo(51);
    }

    @Test
    void validDaysAreAccepted() {
        var days = ProgressSyncService.validate(new SyncRequest(List.of(
                new DayInput("2026-11-14", 12, 3, 1830.6),
                new DayInput("2026-11-13", null, null, null))), SAT);
        assertThat(days).hasSize(2);
        assertThat(days.get(0).seconds()).isEqualTo(1831);
        assertThat(days.get(1).correct()).isZero();
    }

    @Test
    void invalidDaysAreRejected() {
        assertThatThrownBy(() -> ProgressSyncService.validate(null, SAT)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> validate(new DayInput("14.11.2026", 1, 1, 0.0))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> validate(new DayInput("2026-11-20", 1, 1, 0.0))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> validate(new DayInput("2026-11-14", -1, 1, 0.0))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> validate(new DayInput("2026-11-14", 1, 1, -5.0))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> validate(new DayInput("2026-11-14", 1, 1, Double.NaN))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> validate(new DayInput("2026-11-14", 1, 0, 0.0), new DayInput("2026-11-14", 2, 0, 0.0)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void secondsAreCappedAtOneDay() {
        assertThat(ProgressSyncService.validate(new SyncRequest(List.of(new DayInput("2026-11-14", 0, 0, 999_999.0))), SAT)
                .get(0).seconds()).isEqualTo(ProgressSyncService.SECONDS_PER_DAY);
    }

    private static void validate(DayInput... days) {
        ProgressSyncService.validate(new SyncRequest(List.of(days)), SAT);
    }
}
