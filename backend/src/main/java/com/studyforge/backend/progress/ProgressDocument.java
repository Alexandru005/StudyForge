package com.studyforge.backend.progress;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Structura progresului din frontend (progress/store.js):
 * <pre>
 * { "v": 1,
 *   "subjects": { "plan:...": { "title": "Algoritmi" } },
 *   "days": { "2026-11-14": { "plan:...": { "sec": 1830.5, "ok": 12, "bad": 3, "weeks": { "2": { "ok": 12, "bad": 3 } } } } } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProgressDocument(Integer v, Map<String, Subject> subjects, Map<String, Map<String, Bucket>> days) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Subject(String title) {
    }

    /** sec = secunde de studiu activ; ok / bad = raspunsuri "stiut" / "de repetat"; weeks = pe saptamanile din plan. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Bucket(Double sec, Integer ok, Integer bad, Map<String, WeekCount> weeks) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WeekCount(Integer ok, Integer bad) {
    }
}
