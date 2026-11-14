package com.studyforge.backend.progress;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

import com.studyforge.backend.common.ApiException;

/**
 * Saptamanile din clasament: de luni pana duminica.
 * offset 0 = saptamana aceasta, -1 = saptamana trecuta, ... (cel mult {@link #MAX_WEEKS_BACK} in urma).
 */
public final class Weeks {

    public static final int MAX_WEEKS_BACK = 52;

    private Weeks() {
    }

    public static LocalDate monday(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Lunea saptamanii cu offset-ul dat; arunca 400 pentru saptamani din viitor sau prea vechi. */
    public static LocalDate start(LocalDate today, int offset) {
        if (offset > 0) {
            throw ApiException.badRequest("Nu poți vedea clasamentul unei săptămâni din viitor.");
        }
        if (offset < -MAX_WEEKS_BACK) {
            throw ApiException.badRequest("Poți vedea clasamentul cel mult " + MAX_WEEKS_BACK + " de săptămâni în urmă.");
        }
        return monday(today).plusWeeks(offset);
    }

    /** Cate saptamani in urma e data (0 = saptamana aceasta, -1 = trecuta ...). */
    public static int offsetOf(LocalDate today, LocalDate date) {
        return (int) ChronoUnit.WEEKS.between(monday(today), monday(date));
    }
}
