package com.studyforge.backend.pdf;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.studyforge.backend.common.Dates;
import com.studyforge.backend.plan.PlanContent;

/**
 * Genereaza PDF-ul cu planul de invatare pe saptamani.
 */
@Component
public class PlanPdfRenderer {

    public byte[] render(UUID planId, PlanContent plan) {
        String title = nonBlank(plan.title(), "Plan de învățare");
        try (PdfLayout pdf = new PdfLayout("StudyForge · " + title)) {
            pdf.title(title);
            pdf.subtitle(subtitle(plan));

            if (plan.summary() != null && !plan.summary().isBlank()) {
                pdf.paragraph(plan.summary());
            }

            pdf.section("Calendarul evaluărilor");
            List<PlanContent.Assessment> assessments = safe(plan.assessments());
            if (assessments.isEmpty()) {
                pdf.paragraph("Nu au fost identificate evaluări cu dată în programă.");
            } else {
                List<String> rows = new ArrayList<>();
                for (PlanContent.Assessment a : assessments) {
                    rows.add(describe(a));
                }
                pdf.bullets(rows);
            }

            pdf.section("Planul pe săptămâni");
            for (PlanContent.Week week : safe(plan.weeks())) {
                LocalDate start = Dates.parse(week.startDate());
                LocalDate end = Dates.parse(week.endDate());
                pdf.bandHeader("Săptămâna " + week.weekNumber(), Dates.range(start, end));
                if (week.focus() != null && !week.focus().isBlank()) {
                    pdf.emphasis(week.focus());
                }
                if (!safe(week.topics()).isEmpty()) {
                    pdf.label("TEME");
                    pdf.bullets(week.topics());
                }
                if (!safe(week.tasks()).isEmpty()) {
                    pdf.label("ACTIVITĂȚI");
                    pdf.bullets(week.tasks());
                }
                if (!safe(week.assessments()).isEmpty()) {
                    pdf.callout("Evaluare în această săptămână", String.join(", ", week.assessments()));
                }
                pdf.spacer(6f);
            }

            if (!safe(plan.tips()).isEmpty()) {
                pdf.section("Sfaturi");
                pdf.bullets(plan.tips());
            }

            return pdf.finish(title, Map.of(
                    PdfMetadata.PLAN_ID, planId.toString(),
                    PdfMetadata.DOC_TYPE, "plan"));
        } catch (IOException e) {
            throw new UncheckedIOException("Nu am putut genera PDF-ul planului", e);
        }
    }

    private static String subtitle(PlanContent plan) {
        List<String> parts = new ArrayList<>();
        if (plan.subject() != null && !plan.subject().isBlank()) {
            parts.add(plan.subject());
        }
        LocalDate start = Dates.parse(plan.startDate());
        LocalDate end = Dates.parse(plan.endDate());
        if (start != null && end != null) {
            parts.add(Dates.full(start) + " – " + Dates.full(end));
        }
        int weeks = safe(plan.weeks()).size();
        parts.add(weeks + (weeks == 1 ? " săptămână" : " săptămâni"));
        return String.join("  ·  ", parts);
    }

    private static String describe(PlanContent.Assessment a) {
        StringBuilder sb = new StringBuilder();
        LocalDate date = Dates.parse(a.date());
        sb.append(date != null ? Dates.full(date) : "Dată necunoscută").append(" — ");
        String name = nonBlank(a.name(), "Evaluare");
        sb.append(name);
        if (a.type() != null && !a.type().isBlank() && !name.toLowerCase().contains(a.type().toLowerCase())) {
            sb.append(" (").append(a.type()).append(")");
        }
        List<String> coverage = safe(a.coverage());
        if (!coverage.isEmpty()) {
            sb.append(": ").append(String.join("; ", coverage));
        }
        return sb.toString();
    }

    private static String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
