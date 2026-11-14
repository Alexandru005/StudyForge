package com.studyforge.backend.progress;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studyforge.backend.auth.AppUser;
import com.studyforge.backend.auth.AppUserRepository;
import com.studyforge.backend.common.ApiException;
import com.studyforge.backend.config.AppProperties;

/**
 * Progresul se tine in browser (localStorage). Frontend-ul trimite aici totalurile pe zile
 * (adunate pe toate materiile), ca sa poata aparea in clasamentul cu prietenii.
 */
@Service
public class ProgressSyncService {

    static final int MAX_DAYS_PER_REQUEST = 1000;
    static final int MAX_ANSWERS_PER_DAY = 100_000;
    static final int SECONDS_PER_DAY = 86_400;
    private static final LocalDate MIN_DATE = LocalDate.of(2020, 1, 1);

    private final AppUserRepository users;
    private final StudyDayRepository days;
    private final AppProperties properties;

    public ProgressSyncService(AppUserRepository users, StudyDayRepository days, AppProperties properties) {
        this.users = users;
        this.days = days;
        this.properties = properties;
    }

    /** { "days": [ { "date": "2026-11-14", "correct": 12, "wrong": 3, "seconds": 1830.5 } ] } */
    public record SyncRequest(List<DayInput> days) {
    }

    public record DayInput(String date, Integer correct, Integer wrong, Double seconds) {
    }

    public record SyncResult(int saved) {
    }

    /** O zi validata. */
    public record ValidDay(LocalDate date, int correct, int wrong, int seconds) {
    }

    /**
     * Inlocuieste totalurile zilelor trimise. Zilele care nu apar in cerere raman neschimbate,
     * deci frontend-ul poate trimite fie tot istoricul, fie doar ziua de azi.
     */
    @Transactional
    public SyncResult sync(UUID userId, SyncRequest request) {
        LocalDate today = LocalDate.now(properties.zoneId());
        List<ValidDay> valid = validate(request, today);
        if (valid.isEmpty()) {
            return new SyncResult(0);
        }
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "Contul nu mai există. Autentifică-te din nou."));
        applyDays(user, valid);
        return new SyncResult(valid.size());
    }

    /** Scrie totalurile zilelor (insereaza zilele noi, actualizeaza-le pe cele existente). Cere o tranzactie. */
    public void applyDays(AppUser user, List<ValidDay> valid) {
        if (valid.isEmpty()) {
            return;
        }
        Map<LocalDate, StudyDay> existing = new HashMap<>();
        for (StudyDay d : days.findDays(user.getId(), valid.stream().map(ValidDay::date).toList())) {
            existing.put(d.getStudyDate(), d);
        }
        for (ValidDay v : valid) {
            StudyDay day = existing.get(v.date());
            if (day == null) {
                day = days.save(new StudyDay(user, v.date()));
            }
            day.update(v.correct(), v.wrong(), v.seconds());
        }
        try {
            days.flush();
        } catch (DataIntegrityViolationException e) {
            // doua sincronizari simultane au inserat aceeasi zi
            throw new ApiException(HttpStatus.CONFLICT, "Progresul se sincroniza deja. Încearcă din nou.");
        }
    }

    static List<ValidDay> validate(SyncRequest request, LocalDate today) {
        if (request == null || request.days() == null) {
            throw ApiException.badRequest("Trimite lista de zile (\"days\").");
        }
        if (request.days().size() > MAX_DAYS_PER_REQUEST) {
            throw ApiException.badRequest("Poți trimite cel mult " + MAX_DAYS_PER_REQUEST + " de zile odată.");
        }
        Set<LocalDate> seen = new HashSet<>();
        return request.days().stream().map(d -> {
            if (d == null || d.date() == null) {
                throw ApiException.badRequest("Fiecare zi trebuie să aibă o dată (YYYY-MM-DD).");
            }
            LocalDate date;
            try {
                date = LocalDate.parse(d.date().trim());
            } catch (DateTimeParseException e) {
                throw ApiException.badRequest("Data „" + d.date() + "” nu are formatul YYYY-MM-DD.");
            }
            // +1 zi: dispozitivul poate fi intr-un fus orar inaintea serverului
            if (date.isBefore(MIN_DATE) || date.isAfter(today.plusDays(1))) {
                throw ApiException.badRequest("Data " + date + " nu e validă.");
            }
            if (!seen.add(date)) {
                throw ApiException.badRequest("Ziua " + date + " apare de două ori.");
            }
            int correct = count(d.correct());
            int wrong = count(d.wrong());
            if ((long) correct + wrong > MAX_ANSWERS_PER_DAY) {
                throw ApiException.badRequest("Prea multe răspunsuri în ziua " + date + ".");
            }
            double seconds = d.seconds() == null ? 0 : d.seconds();
            if (!(seconds >= 0) || Double.isInfinite(seconds)) {
                throw ApiException.badRequest("Timpul de studiu trebuie să fie un număr pozitiv.");
            }
            int rounded = (int) Math.min(Math.round(seconds), SECONDS_PER_DAY);
            return new ValidDay(date, correct, wrong, rounded);
        }).toList();
    }

    private static int count(Integer value) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw ApiException.badRequest("Numărul de răspunsuri nu poate fi negativ.");
        }
        return value;
    }
}
