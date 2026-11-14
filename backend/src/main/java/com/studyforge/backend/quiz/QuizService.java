package com.studyforge.backend.quiz;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.studyforge.backend.ai.GeminiClient;
import com.studyforge.backend.auth.AppUserRepository;
import com.studyforge.backend.ai.GeminiSchemas;
import com.studyforge.backend.ai.Prompts;
import com.studyforge.backend.common.ApiException;
import com.studyforge.backend.common.Dates;
import com.studyforge.backend.common.FileSupport;
import com.studyforge.backend.common.JsonSupport;
import com.studyforge.backend.config.AppProperties;
import com.studyforge.backend.pdf.FlashcardPdfRenderer;
import com.studyforge.backend.pdf.PdfTextExtractor;
import com.studyforge.backend.plan.LearningPlan;
import com.studyforge.backend.plan.LearningPlanRepository;
import com.studyforge.backend.plan.PlanContent;
import com.studyforge.backend.plan.PlanService;
import com.studyforge.backend.storage.PdfFile;
import com.studyforge.backend.storage.StoredPdf;
import com.studyforge.backend.storage.StoredPdfRepository;

/**
 * Pagina 3: PDF-ul planului + saptamana curenta -> Gemini -> flashcard-uri -> PDF salvat in DB.
 */
@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    public static final int DEFAULT_COUNT = 15;
    public static final int MIN_COUNT = 5;
    public static final int MAX_COUNT = 40;
    private static final int MAX_WEEK_WITHOUT_PLAN = 52;

    private final PdfTextExtractor extractor;
    private final GeminiClient gemini;
    private final FlashcardPdfRenderer renderer;
    private final LearningPlanRepository plans;
    private final QuizRepository quizzes;
    private final StoredPdfRepository pdfs;
    private final AppUserRepository users;
    private final JsonSupport json;
    private final AppProperties properties;
    private final TransactionTemplate tx;

    public QuizService(PdfTextExtractor extractor, GeminiClient gemini, FlashcardPdfRenderer renderer,
            LearningPlanRepository plans, QuizRepository quizzes, StoredPdfRepository pdfs, AppUserRepository users,
            JsonSupport json, AppProperties properties, PlatformTransactionManager transactionManager) {
        this.extractor = extractor;
        this.gemini = gemini;
        this.renderer = renderer;
        this.plans = plans;
        this.quizzes = quizzes;
        this.pdfs = pdfs;
        this.users = users;
        this.json = json;
        this.properties = properties;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /**
     * @param file  PDF-ul planului (cel generat pe pagina 2)
     * @param week  saptamana in care se afla utilizatorul (1, 2, ...)
     * @param count cate flashcard-uri (implicit 15)
     * @param userId utilizatorul autentificat (proprietarul setului)
     */
    public QuizDtos.QuizDetails create(MultipartFile file, Integer week, Integer count, UUID userId) {
        if (week == null || week < 1) {
            throw ApiException.badRequest("Alege săptămâna în care ești (un număr de la 1 în sus).");
        }
        int cardCount = count == null ? DEFAULT_COUNT : count;
        if (cardCount < MIN_COUNT || cardCount > MAX_COUNT) {
            throw ApiException.badRequest(
                    "Numărul de flashcard-uri trebuie să fie între " + MIN_COUNT + " și " + MAX_COUNT + ".");
        }

        byte[] bytes = FileSupport.readPdf(file);
        PdfTextExtractor.Extracted extracted = extractor.extract(bytes);
        if ("flashcards".equals(extracted.docType())) {
            throw ApiException.badRequest(
                    "Acesta este un PDF cu flashcard-uri. Încarcă PDF-ul cu planul de învățare.");
        }

        LearningPlan plan = resolvePlan(extracted.planId(), FileSupport.sha256(bytes), userId).orElse(null);
        PlanContent planContent = plan != null && plan.getContentJson() != null
                ? json.read(plan.getContentJson(), PlanContent.class)
                : null;

        String prompt;
        String stageLabel;
        String dateRange = "";
        String subject = null;
        if (planContent != null && planContent.weeks() != null && !planContent.weeks().isEmpty()) {
            List<PlanContent.Week> weeks = planContent.weeks();
            if (week > weeks.size()) {
                throw ApiException.badRequest("Planul are doar " + weeks.size()
                        + (weeks.size() == 1 ? " săptămână." : " săptămâni."));
            }
            PlanContent.Week selected = weeks.stream()
                    .filter(w -> week.equals(w.weekNumber()))
                    .findFirst()
                    .orElse(weeks.get(week - 1));
            prompt = Prompts.flashcardsFromPlanWeek(planContent, selected, cardCount);
            stageLabel = "Săptămâna " + week + (selected.focus() != null ? " · " + selected.focus() : "");
            dateRange = Dates.range(Dates.parse(selected.startDate()), Dates.parse(selected.endDate()));
            subject = planContent.subject();
            log.info("Generez {} flashcard-uri pentru planul {} saptamana {}", cardCount, plan.getId(), week);
        } else {
            if (week > MAX_WEEK_WITHOUT_PLAN) {
                throw ApiException.badRequest("Săptămâna trebuie să fie între 1 și " + MAX_WEEK_WITHOUT_PLAN + ".");
            }
            String text = extracted.text();
            if (text.length() > properties.maxInputChars()) {
                text = text.substring(0, properties.maxInputChars());
            }
            prompt = Prompts.flashcardsFromPlanText(text, week, cardCount);
            stageLabel = "Săptămâna " + week;
            log.info("PDF necunoscut ({}). Generez flashcard-uri direct din text, saptamana {}",
                    file.getOriginalFilename(), week);
        }

        FlashcardContent generated = gemini.generateJson(Prompts.SYSTEM, prompt, GeminiSchemas.flashcards(),
                FlashcardContent.class);
        List<FlashcardPdfRenderer.Card> cards = clean(generated, cardCount);
        if (cards.isEmpty()) {
            throw ApiException.unprocessable("Nu am găsit conținut pentru săptămâna " + week
                    + " în acest PDF. Verifică dacă ai încărcat planul corect.");
        }
        if (planContent == null && generated.weekLabel() != null && !generated.weekLabel().isBlank()) {
            stageLabel = generated.weekLabel().trim();
        }

        UUID planId = plan != null ? plan.getId() : null;
        String planTitle = plan != null ? plan.getTitle() : null;
        byte[] pdfBytes = renderer.render(planId, subject, stageLabel, dateRange, cards);
        String fileName = "flashcards-" + FileSupport.slug(subject != null ? subject : "plan")
                + "-saptamana-" + week + "-" + LocalDate.now(properties.zoneId()) + ".pdf";
        StoredPdf stored = new StoredPdf(fileName, pdfBytes);
        String sourceName = FileSupport.originalName(file, "plan.pdf");
        String finalStageLabel = stageLabel;

        Quiz quiz = tx.execute(status -> {
            LearningPlan planRef = planId != null ? plans.getReferenceById(planId) : null;
            Quiz q = new Quiz(PlanService.ownerReference(users, userId), planRef, finalStageLabel, week, sourceName);
            for (FlashcardPdfRenderer.Card card : cards) {
                q.addFlashcard(card.question(), card.answer());
            }
            pdfs.save(stored);
            q.attachPdf(stored);
            return quizzes.save(q);
        });

        log.info("Set flashcard-uri {} salvat ({} carduri)", quiz.getId(), cards.size());
        List<QuizDtos.FlashcardItem> items = quiz.getFlashcards().stream().map(QuizDtos.FlashcardItem::of).toList();
        return new QuizDtos.QuizDetails(QuizDtos.QuizSummary.of(quiz, planId, planTitle, items.size()), items);
    }

    @Transactional(readOnly = true)
    public List<QuizDtos.QuizSummary> list(UUID planId, UUID userId) {
        List<Quiz> found = planId != null
                ? quizzes.findAllByOwnerAndPlan(userId, planId)
                : quizzes.findAllByOwner(userId);
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : quizzes.countFlashcardsPerQuiz(userId)) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return found.stream().map(q -> summary(q, counts.getOrDefault(q.getId(), 0L))).toList();
    }

    @Transactional(readOnly = true)
    public QuizDtos.QuizDetails get(UUID id, UUID userId) {
        Quiz quiz = quizzes.findWithFlashcards(id, userId)
                .orElseThrow(() -> ApiException.notFound("Setul de flashcard-uri nu a fost găsit."));
        List<QuizDtos.FlashcardItem> items = quiz.getFlashcards().stream()
                .sorted(Comparator.comparingInt(Flashcard::getOrderIndex))
                .map(QuizDtos.FlashcardItem::of)
                .toList();
        return new QuizDtos.QuizDetails(summary(quiz, items.size()), items);
    }

    @Transactional(readOnly = true)
    public PdfFile pdf(UUID id, UUID userId) {
        Quiz quiz = quizzes.findByIdAndOwner(id, userId)
                .orElseThrow(() -> ApiException.notFound("Setul de flashcard-uri nu a fost găsit."));
        if (quiz.getPdf() == null) {
            throw ApiException.notFound("Setul nu are un PDF salvat.");
        }
        StoredPdf stored = pdfs.findById(quiz.getPdf().getId())
                .orElseThrow(() -> ApiException.notFound("PDF-ul cu flashcard-uri nu a fost găsit."));
        return new PdfFile(stored.getFileName(), stored.getContent());
    }

    private QuizDtos.QuizSummary summary(Quiz quiz, long count) {
        LearningPlan plan = quiz.getLearningPlan();
        return QuizDtos.QuizSummary.of(quiz, plan != null ? plan.getId() : null,
                plan != null ? plan.getTitle() : null, count);
    }

    /**
     * Planul e recunoscut dupa ID-ul din metadatele PDF-ului sau, ca rezerva, dupa hash-ul fisierului.
     * Se cauta doar printre planurile utilizatorului curent.
     */
    private Optional<LearningPlan> resolvePlan(String planIdFromMetadata, String sha256, UUID userId) {
        return tx.execute(status -> {
            if (planIdFromMetadata != null) {
                try {
                    Optional<LearningPlan> byId = plans.findByIdAndOwner(
                            UUID.fromString(planIdFromMetadata.trim()), userId);
                    if (byId.isPresent()) {
                        return byId;
                    }
                } catch (IllegalArgumentException ignored) {
                    // metadate invalide -> incercam dupa hash
                }
            }
            return plans.findFirstByPdfSha256AndOwner(sha256, userId);
        });
    }

    private static List<FlashcardPdfRenderer.Card> clean(FlashcardContent content, int max) {
        List<FlashcardPdfRenderer.Card> cards = new ArrayList<>();
        if (content == null || content.flashcards() == null) {
            return cards;
        }
        Set<String> seen = new HashSet<>();
        for (FlashcardContent.Card card : content.flashcards()) {
            if (card == null || card.question() == null || card.answer() == null) {
                continue;
            }
            String question = card.question().trim();
            String answer = card.answer().trim();
            if (question.isEmpty() || answer.isEmpty()) {
                continue;
            }
            if (seen.add(question.toLowerCase().replaceAll("\\s+", " "))) {
                cards.add(new FlashcardPdfRenderer.Card(question, answer));
            }
            if (cards.size() >= max) {
                break;
            }
        }
        return cards;
    }
}
