package com.studyforge.backend.plan;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Structura planului de invatare. Vine de la Gemini (JSON), e completata/normalizata in PlanService,
 * salvata in learning_plan.content_json si folosita la generarea PDF-ului si a flashcard-urilor.
 * Datele sunt string-uri ISO (YYYY-MM-DD).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlanContent(
        String title,
        String subject,
        String summary,
        String startDate,
        String endDate,
        List<Assessment> assessments,
        List<Week> weeks,
        List<String> tips) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Assessment(String name, String date, String type, List<String> coverage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Week(
            Integer weekNumber,
            String startDate,
            String endDate,
            String focus,
            List<String> topics,
            List<String> tasks,
            List<String> assessments) {
    }
}
