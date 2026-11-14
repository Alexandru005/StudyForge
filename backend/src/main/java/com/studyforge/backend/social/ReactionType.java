package com.studyforge.backend.social;

import java.util.Locale;

import com.studyforge.backend.common.ApiException;

/** Cele doua butoane din clasament. In JSON: "LIKE" sau "DISLIKE". */
public enum ReactionType {
    LIKE,
    DISLIKE;

    /** Accepta si litere mici ("like"); arunca 400 pentru orice alta valoare. */
    public static ReactionType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.badRequest("Alege reacția: LIKE sau DISLIKE.");
        }
        try {
            return ReactionType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Reacție invalidă. Valori permise: LIKE, DISLIKE.");
        }
    }
}
