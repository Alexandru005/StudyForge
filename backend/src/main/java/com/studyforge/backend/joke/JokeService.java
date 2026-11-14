package com.studyforge.backend.joke;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.studyforge.backend.ai.GeminiClient;
import com.studyforge.backend.ai.GeminiSchemas;
import com.studyforge.backend.ai.Prompts;
import com.studyforge.backend.common.ApiException;
import com.studyforge.backend.common.JsonSupport;
import com.studyforge.backend.config.AppProperties;
import com.studyforge.backend.plan.LearningPlan;
import com.studyforge.backend.plan.LearningPlanRepository;
import com.studyforge.backend.plan.PlanContent;
import com.studyforge.backend.progress.StudyDayRepository;
import com.studyforge.backend.progress.WeekStats;
import com.studyforge.backend.progress.Weeks;

/**
 * Butonul „Glumă pasiv-agresivă din materie”: o gluma scurta generata de Gemini din planul de studiu
 * (materia, saptamana curenta, evaluarile care urmeaza) si din scorul tau de saptamana aceasta.
 * Gluma NU se salveaza in baza de date.
 */
@Service
public class JokeService {

    private final LearningPlanRepository plans;
    private final StudyDayRepository days;
    private final GeminiClient gemini;
    private final JsonSupport json;
    private final AppProperties properties;

    public JokeService(LearningPlanRepository plans, StudyDayRepository days, GeminiClient gemini, JsonSupport json,
            AppProperties properties) {
        this.plans = plans;
        this.days = days;
        this.gemini = gemini;
        this.json = json;
        this.properties = properties;
    }

    /** { "planId": "..." (optional, implicit ultimul plan), "previousJoke": "..." (optional, pentru „Altă glumă”) } */
    public record JokeRequest(UUID planId, String previousJoke) {
    }

    /** subject = materia din care e gluma (eticheta de sub glumă). */
    public record JokeResponse(String joke, String subject, UUID planId) {
    }

    @Transactional(readOnly = true)
    public JokeResponse generate(UUID userId, JokeRequest request) {
        UUID planId = request == null ? null : request.planId();
        LearningPlan plan = planId != null
                ? plans.findByIdAndOwner(planId, userId)
                        .orElseThrow(() -> ApiException.notFound("Planul nu a fost găsit."))
                : plans.findAllByOwner(userId).stream().findFirst()
                        .orElseThrow(() -> ApiException.notFound(
                                "Generează întâi un plan de studiu, ca să am din ce materie să glumesc."));

        PlanContent content = plan.getContentJson() == null ? null : json.read(plan.getContentJson(), PlanContent.class);
        String subject = firstNonBlank(plan.getSubject(), content == null ? null : content.subject(), plan.getTitle());
        LocalDate today = LocalDate.now(properties.zoneId());
        LocalDate monday = Weeks.monday(today);
        WeekStats week = days.totals(List.of(userId), monday, monday.plusDays(6)).stream()
                .findFirst()
                .map(r -> WeekStats.of(num(r[1]), num(r[2]), num(r[3])))
                .orElse(WeekStats.EMPTY);

        String prompt = prompt(subject, content, today, week, request == null ? null : request.previousJoke());
        JokeText answer = gemini.generateJson(Prompts.SYSTEM, prompt, GeminiSchemas.joke(), JokeText.class);
        if (answer == null || answer.joke() == null || answer.joke().isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Nu am putut genera o glumă. Încearcă din nou.");
        }
        return new JokeResponse(answer.joke().trim(), subject, plan.getId());
    }

    static String prompt(String subject, PlanContent content, LocalDate today, WeekStats week, String previousJoke) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("Materia: ").append(subject).append('\n');
        ctx.append("Data de azi: ").append(today).append('\n');
        if (content != null && content.weeks() != null) {
            content.weeks().stream()
                    .filter(w -> covers(w, today))
                    .findFirst()
                    .ifPresent(w -> ctx.append("Săptămâna curentă din plan (").append(w.weekNumber()).append("): ")
                            .append(orEmpty(w.focus()))
                            .append(w.topics() == null || w.topics().isEmpty() ? "" : " — teme: " + String.join(", ", w.topics()))
                            .append('\n'));
            List<String> topics = content.weeks().stream()
                    .filter(w -> w.topics() != null)
                    .flatMap(w -> w.topics().stream())
                    .limit(25)
                    .toList();
            if (!topics.isEmpty()) {
                ctx.append("Teme din programă: ").append(String.join(", ", topics)).append('\n');
            }
        }
        if (content != null && content.assessments() != null) {
            content.assessments().stream()
                    .filter(a -> a.date() != null && !parse(a.date()).isBefore(today))
                    .limit(3)
                    .forEach(a -> ctx.append("Evaluare care urmează: ").append(orEmpty(a.name())).append(" pe ")
                            .append(a.date()).append('\n'));
        }
        ctx.append(week.score() == null
                ? "Săptămâna aceasta studentul nu a rezolvat niciun flashcard.\n"
                : "Săptămâna aceasta: " + week.correct() + " din " + week.answered() + " flashcard-uri corecte ("
                        + week.score() + "%).\n");
        String avoid = previousJoke == null || previousJoke.isBlank()
                ? ""
                : "Gluma anterioară a fost: „" + previousJoke.trim() + "”. Scrie una DIFERITĂ, despre alt concept.\n";

        return """
                %s
                %s
                Scrie O SINGURĂ glumă scurtă, pasiv-agresivă dar cu drag, ca de la un prieten care te tachinează
                să înveți. Pornește de la un concept concret din materie (folosește corect termenul) și leag-o de
                situația studentului (scorul, evaluarea care urmează sau amânarea studiului).
                Reguli:
                - maximum 2 propoziții, în limba română, cu diacritice;
                - fără jigniri, fără limbaj vulgar, fără să faci de râs studentul pe bune;
                - nu inventa date sau evaluări care nu apar mai sus.
                """.formatted(ctx.toString().trim(), avoid);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record JokeText(String joke) {
    }

    private static boolean covers(PlanContent.Week w, LocalDate day) {
        LocalDate from = parse(w.startDate());
        LocalDate to = parse(w.endDate());
        return from != LocalDate.MIN && to != LocalDate.MIN && !day.isBefore(from) && !day.isAfter(to);
    }

    private static LocalDate parse(String iso) {
        try {
            return iso == null ? LocalDate.MIN : LocalDate.parse(iso.trim());
        } catch (DateTimeParseException e) {
            return LocalDate.MIN;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "materia ta";
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static long num(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }
}
