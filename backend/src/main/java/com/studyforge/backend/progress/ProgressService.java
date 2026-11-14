package com.studyforge.backend.progress;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studyforge.backend.auth.AppUser;
import com.studyforge.backend.auth.AppUserRepository;
import com.studyforge.backend.common.ApiException;
import com.studyforge.backend.common.JsonSupport;
import com.studyforge.backend.config.AppProperties;
import com.studyforge.backend.progress.ProgressDocument.Bucket;
import com.studyforge.backend.progress.ProgressDocument.Subject;
import com.studyforge.backend.progress.ProgressDocument.WeekCount;
import com.studyforge.backend.progress.ProgressSyncService.ValidDay;

/**
 * Progresul complet (pagina "Progres") salvat pe server, ca sa nu se piarda odata cu datele browserului.
 * La salvare se actualizeaza si totalurile pe zile folosite de clasamentul cu prietenii.
 */
@Service
public class ProgressService {

    static final int MAX_SUBJECTS = 1000;
    static final int MAX_DAYS = 3000;
    static final int MAX_TITLE = 300;
    static final int MAX_WEEK = 104;
    static final int MAX_JSON_CHARS = 2_000_000;
    private static final LocalDate MIN_DATE = LocalDate.of(2020, 1, 1);

    private final AppUserRepository users;
    private final UserProgressRepository progress;
    private final ProgressSyncService daySync;
    private final JsonSupport json;
    private final AppProperties properties;

    public ProgressService(AppUserRepository users, UserProgressRepository progress, ProgressSyncService daySync,
            JsonSupport json, AppProperties properties) {
        this.users = users;
        this.progress = progress;
        this.daySync = daySync;
        this.json = json;
        this.properties = properties;
    }

    /** data = null daca utilizatorul nu a salvat inca nimic pe server. */
    public record ProgressResponse(ProgressDocument data, Instant updatedAt) {
    }

    @Transactional(readOnly = true)
    public ProgressResponse get(UUID userId) {
        return progress.findByUser(userId)
                .map(p -> new ProgressResponse(json.read(p.getData(), ProgressDocument.class), p.getUpdatedAt()))
                .orElse(new ProgressResponse(null, null));
    }

    /** Inlocuieste progresul salvat cu cel trimis (frontend-ul il imbina inainte cu cel de pe server). */
    @Transactional
    public ProgressResponse save(UUID userId, ProgressDocument document) {
        LocalDate today = LocalDate.now(properties.zoneId());
        ProgressDocument clean = normalize(document, today);
        String data = json.write(clean);
        if (data.length() > MAX_JSON_CHARS) {
            throw ApiException.badRequest("Progresul e prea mare ca să fie salvat.");
        }
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "Contul nu mai există. Autentifică-te din nou."));

        UserProgress saved = progress.findByUser(userId).orElse(null);
        if (saved == null) {
            saved = new UserProgress(user, data);
            try {
                progress.saveAndFlush(saved);
            } catch (DataIntegrityViolationException e) {
                throw new ApiException(HttpStatus.CONFLICT, "Progresul se salva deja. Încearcă din nou.");
            }
        } else {
            saved.replace(data);
        }
        daySync.applyDays(user, dayTotals(clean));
        return new ProgressResponse(clean, saved.getUpdatedAt());
    }

    // ------------------------------------------------------------------ validare (statica, testabila)

    /** Verifica si curata documentul: valori negative / formate gresite -> 400; zilele din viitor se ignora. */
    static ProgressDocument normalize(ProgressDocument doc, LocalDate today) {
        if (doc == null || doc.v() == null || doc.v() != 1) {
            throw ApiException.badRequest("Progresul trimis nu are formatul așteptat (v: 1).");
        }
        Map<String, Subject> subjects = new TreeMap<>();
        if (doc.subjects() != null) {
            if (doc.subjects().size() > MAX_SUBJECTS) {
                throw ApiException.badRequest("Prea multe materii în progres.");
            }
            doc.subjects().forEach((key, s) -> {
                if (key == null || key.isBlank() || key.length() > MAX_TITLE) {
                    throw ApiException.badRequest("Cheie de materie invalidă în progres.");
                }
                String title = s == null || s.title() == null || s.title().isBlank() ? key : s.title().trim();
                subjects.put(key, new Subject(title.length() > MAX_TITLE ? title.substring(0, MAX_TITLE) : title));
            });
        }

        Map<String, Map<String, Bucket>> days = new TreeMap<>();
        if (doc.days() != null) {
            if (doc.days().size() > MAX_DAYS) {
                throw ApiException.badRequest("Progresul are prea multe zile.");
            }
            doc.days().forEach((dayKey, perSubject) -> {
                LocalDate date = parseDay(dayKey);
                if (date.isBefore(MIN_DATE) || date.isAfter(today.plusDays(1)) || perSubject == null) {
                    return;
                }
                Map<String, Bucket> clean = new TreeMap<>();
                perSubject.forEach((subjectKey, b) -> {
                    if (subjectKey == null || subjectKey.isBlank() || subjectKey.length() > MAX_TITLE || b == null) {
                        return;
                    }
                    clean.put(subjectKey, bucket(b));
                    subjects.putIfAbsent(subjectKey, new Subject(subjectKey));
                });
                if (!clean.isEmpty()) {
                    days.put(date.toString(), clean);
                }
            });
        }
        return new ProgressDocument(1, subjects, days);
    }

    /** Totalurile pe zi (toate materiile adunate), pentru clasament. */
    static List<ValidDay> dayTotals(ProgressDocument doc) {
        List<ValidDay> result = new ArrayList<>();
        doc.days().forEach((day, perSubject) -> {
            long ok = 0;
            long bad = 0;
            double sec = 0;
            for (Bucket b : perSubject.values()) {
                ok += b.ok();
                bad += b.bad();
                sec += b.sec();
            }
            int correct = (int) Math.min(ok, ProgressSyncService.MAX_ANSWERS_PER_DAY);
            int wrong = (int) Math.min(bad, ProgressSyncService.MAX_ANSWERS_PER_DAY - correct);
            int seconds = (int) Math.min(Math.round(sec), ProgressSyncService.SECONDS_PER_DAY);
            result.add(new ValidDay(LocalDate.parse(day), correct, wrong, seconds));
        });
        return result;
    }

    private static LocalDate parseDay(String key) {
        try {
            return LocalDate.parse(key == null ? "" : key.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("Ziua „" + key + "” din progres nu are formatul YYYY-MM-DD.");
        }
    }

    private static Bucket bucket(Bucket b) {
        double sec = b.sec() == null ? 0 : b.sec();
        if (!(sec >= 0) || Double.isInfinite(sec)) {
            throw ApiException.badRequest("Timpul de studiu din progres trebuie să fie un număr pozitiv.");
        }
        sec = Math.min(Math.round(sec * 10) / 10.0, ProgressSyncService.SECONDS_PER_DAY);
        Map<String, WeekCount> weeks = new TreeMap<>();
        if (b.weeks() != null) {
            b.weeks().forEach((w, c) -> {
                int n;
                try {
                    n = Integer.parseInt(w);
                } catch (NumberFormatException e) {
                    return;
                }
                if (n >= 1 && n <= MAX_WEEK && c != null) {
                    weeks.put(String.valueOf(n), new WeekCount(count(c.ok()), count(c.bad())));
                }
            });
        }
        return new Bucket(sec, count(b.ok()), count(b.bad()), weeks);
    }

    private static int count(Integer value) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw ApiException.badRequest("Numărul de răspunsuri din progres nu poate fi negativ.");
        }
        return Math.min(value, ProgressSyncService.MAX_ANSWERS_PER_DAY);
    }
}
