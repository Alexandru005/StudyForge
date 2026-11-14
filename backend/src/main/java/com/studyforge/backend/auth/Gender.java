package com.studyforge.backend.auth;

import java.util.Locale;

import com.studyforge.backend.common.ApiException;

/** Genul utilizatorului. In JSON se trimite ca "MALE", "FEMALE" sau "OTHER". */
public enum Gender {
    MALE,
    FEMALE,
    OTHER;

    /** Accepta si litere mici ("male"); arunca 400 pentru orice alta valoare. */
    public static Gender parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.badRequest("Alege genul: MALE, FEMALE sau OTHER.");
        }
        try {
            return Gender.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Gen invalid. Valori permise: MALE, FEMALE, OTHER.");
        }
    }
}
