package com.studyforge.backend.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Formatari de date in stil romanesc. */
public final class Dates {

    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd.MM");

    private Dates() {
    }

    /** Parseaza o data ISO (YYYY-MM-DD); null daca lipseste sau e invalida. */
    public static LocalDate parse(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(iso.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static String full(LocalDate date) {
        return date == null ? "" : FULL.format(date);
    }

    /** "17.11 – 23.11.2026" sau "29.12.2026 – 04.01.2027" daca anul difera. */
    public static String range(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return "";
        }
        String left = start.getYear() == end.getYear() ? SHORT.format(start) : FULL.format(start);
        return left + " – " + FULL.format(end);
    }
}
