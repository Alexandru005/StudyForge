package com.studyforge.backend.plan;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.studyforge.backend.ai.GeminiClient;
import com.studyforge.backend.auth.AppUser;
import com.studyforge.backend.auth.AppUserRepository;
import com.studyforge.backend.ai.GeminiSchemas;
import com.studyforge.backend.ai.Prompts;
import com.studyforge.backend.common.ApiException;
import com.studyforge.backend.common.Dates;
import com.studyforge.backend.common.FileSupport;
import com.studyforge.backend.common.JsonSupport;
import com.studyforge.backend.config.AppProperties;
import com.studyforge.backend.pdf.PdfTextExtractor;
import com.studyforge.backend.pdf.PlanPdfRenderer;
import com.studyforge.backend.storage.PdfFile;
import com.studyforge.backend.storage.StoredPdf;
import com.studyforge.backend.storage.StoredPdfRepository;

/**
 * Pagina 2: programa (PDF) -> Gemini -> plan pe saptamani -> PDF salvat in DB.
 */
@Service
public class PlanService {

    private static final Logger log = LoggerFactory.getLogger(PlanService.class);
    static final int MAX_WEEKS = 30;

    private final PdfTextExtractor extractor;
    private final GeminiClient gemini;
    private final PlanPdfRenderer renderer;
    private final LearningPlanRepository plans;
    private final StoredPdfRepository pdfs;
    private final AppUserRepository users;
    private final JsonSupport json;
    private final AppProperties properties;
    private final TransactionTemplate tx;

    public PlanService(PdfTextExtractor extractor, GeminiClient gemini, PlanPdfRenderer renderer,
            LearningPlanRepository plans, StoredPdfRepository pdfs, AppUserRepository users, JsonSupport json,
            AppProperties properties, PlatformTransactionManager transactionManager) {
        this.extractor = extractor;
        this.gemini = gemini;
        this.renderer = renderer;
        this.plans = plans;
        this.pdfs = pdfs;
        this.users = users;
        this.json = json;
        this.properties = properties;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /**
     * @param file           PDF-ul cu programa + evaluarile
     * @param requestedStart data de inceput (optional); se rotunjeste la lunea saptamanii respective
     * @param userId         utilizatorul autentificat (proprietarul planului)
     */
    public PlanDtos.PlanDetails create(MultipartFile file, LocalDate requestedStart, UUID userId) {
        byte[] bytes = FileSupport.readPdf(file);
        PdfTextExtractor.Extracted extracted = extractor.extract(bytes);
        if (extracted.docType() != null) {
            throw ApiException.badRequest(
                    "Acesta este un PDF generat de StudyForge. Pe această pagină încarcă PDF-ul cu programa materiei.");
        }

        LocalDate today = LocalDate.now(properties.zoneId());
        LocalDate start = (requestedStart != null ? requestedStart : today)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        String text = limit(extracted.text(), properties.maxInputChars());
        log.info("Generez plan din '{}' ({} pagini, {} caractere), start {}", file.getOriginalFilename(),
                extracted.pageCount(), text.length(), start);

        PlanContent raw = gemini.generateJson(Prompts.SYSTEM, Prompts.plan(text, today, start, MAX_WEEKS),
                GeminiSchemas.plan(), PlanContent.class);
        PlanContent content = normalize(raw, start);

        LearningPlan plan = new LearningPlan(
                content.title(),
                content.subject(),
                FileSupport.originalName(file, "programa.pdf"),
                start,
                LocalDate.parse(content.endDate()),
                content.weeks().size(),
                json.write(content));

        byte[] pdfBytes = renderer.render(plan.getId(), content);
        StoredPdf stored = new StoredPdf(
                "plan-" + FileSupport.slug(content.subject() != null ? content.subject() : content.title())
                        + "-" + today + ".pdf",
                pdfBytes);
        plan.attachPdf(stored);

        tx.executeWithoutResult(status -> {
            plan.assignOwner(ownerReference(users, userId));
            pdfs.save(stored);
            plans.save(plan);
        });
        log.info("Plan {} salvat ({} saptamani, PDF {} KB)", plan.getId(), content.weeks().size(),
                pdfBytes.length / 1024);
        return new PlanDtos.PlanDetails(PlanDtos.PlanSummary.of(plan), content);
    }

    @Transactional(readOnly = true)
    public List<PlanDtos.PlanSummary> list(UUID userId) {
        return plans.findAllByOwner(userId).stream().map(PlanDtos.PlanSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public PlanDtos.PlanDetails get(UUID id, UUID userId) {
        LearningPlan plan = find(id, userId);
        PlanContent content = plan.getContentJson() != null ? json.read(plan.getContentJson(), PlanContent.class) : null;
        return new PlanDtos.PlanDetails(PlanDtos.PlanSummary.of(plan), content);
    }

    @Transactional(readOnly = true)
    public PdfFile pdf(UUID id, UUID userId) {
        LearningPlan plan = find(id, userId);
        if (plan.getPdf() == null) {
            throw ApiException.notFound("Planul nu are un PDF salvat.");
        }
        StoredPdf stored = pdfs.findById(plan.getPdf().getId())
                .orElseThrow(() -> ApiException.notFound("PDF-ul planului nu a fost găsit."));
        return new PdfFile(stored.getFileName(), stored.getContent());
    }

    /** Un plan al altui utilizator se comporta ca unul inexistent (404). */
    private LearningPlan find(UUID id, UUID userId) {
        return plans.findByIdAndOwner(id, userId)
                .orElseThrow(() -> ApiException.notFound("Planul nu a fost găsit."));
    }

    /** Referinta catre utilizator (fara SELECT complet); 401 daca intre timp contul a fost sters. */
    public static AppUser ownerReference(AppUserRepository users, UUID userId) {
        if (!users.existsById(userId)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Contul nu mai există. Autentifică-te din nou.");
        }
        return users.getReferenceById(userId);
    }

    /**
     * Curata raspunsul Gemini si calculeaza noi datele saptamanilor (deterministe, pornind de la luni),
     * apoi pune fiecare evaluare cu data cunoscuta in saptamana corecta.
     */
    static PlanContent normalize(PlanContent raw, LocalDate startMonday) {
        if (raw == null || raw.weeks() == null || raw.weeks().stream().allMatch(w -> w == null)) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Gemini nu a generat săptămânile planului. Încearcă din nou.");
        }

        List<PlanContent.Assessment> assessments = new ArrayList<>();
        if (raw.assessments() != null) {
            for (PlanContent.Assessment a : raw.assessments()) {
                if (a == null || isBlank(a.name())) {
                    continue;
                }
                LocalDate date = Dates.parse(a.date());
                assessments.add(new PlanContent.Assessment(a.name().trim(), date != null ? date.toString() : "",
                        trimOrNull(a.type()), clean(a.coverage())));
            }
            assessments.sort((a, b) -> {
                if (a.date().isEmpty() || b.date().isEmpty()) {
                    return a.date().isEmpty() == b.date().isEmpty() ? 0 : (a.date().isEmpty() ? 1 : -1);
                }
                return a.date().compareTo(b.date());
            });
        }

        List<PlanContent.Week> sourceWeeks = raw.weeks().stream().filter(w -> w != null).limit(MAX_WEEKS).toList();
        int count = sourceWeeks.size();
        List<Set<String>> weekAssessments = new ArrayList<>();
        for (PlanContent.Week w : sourceWeeks) {
            weekAssessments.add(new LinkedHashSet<>(clean(w.assessments())));
        }
        // Evaluarile cu data cunoscuta merg in saptamana in care cade data
        for (PlanContent.Assessment a : assessments) {
            LocalDate date = Dates.parse(a.date());
            if (date == null || date.isBefore(startMonday)) {
                continue;
            }
            long index = ChronoUnit.DAYS.between(startMonday, date) / 7;
            if (index < count) {
                Set<String> names = weekAssessments.get((int) index);
                boolean present = names.stream().anyMatch(n -> n.equalsIgnoreCase(a.name()));
                if (!present) {
                    names.add(a.name());
                }
            }
        }

        List<PlanContent.Week> weeks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PlanContent.Week w = sourceWeeks.get(i);
            LocalDate weekStart = startMonday.plusWeeks(i);
            weeks.add(new PlanContent.Week(
                    i + 1,
                    weekStart.toString(),
                    weekStart.plusDays(6).toString(),
                    trimOrNull(w.focus()),
                    clean(w.topics()),
                    clean(w.tasks()),
                    List.copyOf(weekAssessments.get(i))));
        }

        String subject = trimOrNull(raw.subject());
        String title = trimOrNull(raw.title());
        if (title == null) {
            title = subject != null ? "Plan de învățare – " + subject : "Plan de învățare";
        }
        LocalDate end = startMonday.plusWeeks(count).minusDays(1);
        return new PlanContent(title, subject, trimOrNull(raw.summary()), startMonday.toString(), end.toString(),
                assessments, weeks, clean(raw.tips()));
    }

    private static List<String> clean(List<String> items) {
        if (items == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String item : items) {
            if (!isBlank(item)) {
                result.add(item.trim());
            }
        }
        return result;
    }

    private static String trimOrNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String limit(String text, int maxChars) {
        return text.length() <= maxChars ? text : text.substring(0, maxChars);
    }
}
