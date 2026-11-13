package com.studyforge.backend.plan;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Obiectele JSON returnate de /api/plans. */
public final class PlanDtos {

    private PlanDtos() {
    }

    public record PlanSummary(
            UUID id,
            String title,
            String subject,
            String syllabusFileName,
            LocalDate startDate,
            LocalDate endDate,
            int weekCount,
            Instant createdAt,
            String pdfUrl,
            String downloadUrl) {

        static PlanSummary of(LearningPlan plan) {
            String base = "/api/plans/" + plan.getId() + "/pdf";
            return new PlanSummary(plan.getId(), plan.getTitle(), plan.getSubject(), plan.getSyllabusFileName(),
                    plan.getStartDate(), plan.getEndDate(), plan.getWeekCount(), plan.getCreatedAt(),
                    base, base + "?download=true");
        }
    }

    /** Planul + continutul complet (saptamani, evaluari, sfaturi) pentru afisare in UI. */
    public record PlanDetails(PlanSummary plan, PlanContent content) {
    }
}
