package com.studyforge.backend.social;

import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studyforge.backend.auth.AppUser;
import com.studyforge.backend.auth.AppUserRepository;
import com.studyforge.backend.common.ApiException;
import com.studyforge.backend.config.AppProperties;
import com.studyforge.backend.progress.StudyDayRepository;
import com.studyforge.backend.progress.WeekStats;
import com.studyforge.backend.progress.Weeks;
import com.studyforge.backend.social.SocialDtos.LeaderboardEntry;
import com.studyforge.backend.social.SocialDtos.ReactionCounts;
import com.studyforge.backend.social.SocialDtos.UserSummary;

/**
 * Clasamentul pe saptamani (luni - duminica): tu + prietenii tai, dupa procentajul de raspunsuri corecte
 * la flashcards, la toate materiile. Plus like / dislike la procentajul fiecaruia.
 */
@Service
public class LeaderboardService {

    private final AppUserRepository users;
    private final FriendshipRepository friendships;
    private final FriendReactionRepository reactions;
    private final StudyDayRepository days;
    private final FriendService friendService;
    private final AppProperties properties;

    public LeaderboardService(AppUserRepository users, FriendshipRepository friendships,
            FriendReactionRepository reactions, StudyDayRepository days, FriendService friendService,
            AppProperties properties) {
        this.users = users;
        this.friendships = friendships;
        this.reactions = reactions;
        this.days = days;
        this.friendService = friendService;
        this.properties = properties;
    }

    // ------------------------------------------------------------------ clasament

    /** @param weekOffset 0 = saptamana aceasta, -1 = trecuta ... */
    @Transactional(readOnly = true)
    public SocialDtos.Leaderboard leaderboard(UUID me, int weekOffset) {
        LocalDate today = today();
        LocalDate start = Weeks.start(today, weekOffset);
        LocalDate end = start.plusDays(6);

        List<AppUser> people = new ArrayList<>();
        people.add(users.findById(me).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                "Contul nu mai există. Autentifică-te din nou.")));
        for (Friendship f : friendships.findAllOfUser(me, FriendshipStatus.ACCEPTED)) {
            people.add(f.other(me));
        }
        List<UUID> ids = people.stream().map(AppUser::getId).toList();

        Map<UUID, WeekStats> stats = new HashMap<>();
        for (Object[] row : days.totals(ids, start, end)) {
            stats.put((UUID) row[0], WeekStats.of(num(row[1]), num(row[2]), num(row[3])));
        }
        Map<UUID, ReactionCounts> received = countReceived(ids, start);
        Map<UUID, ReactionType> given = new HashMap<>();
        for (Object[] row : reactions.findGivenBy(me, start)) {
            given.put((UUID) row[0], (ReactionType) row[1]);
        }

        List<Row> rows = people.stream()
                .map(u -> new Row(UserSummary.of(u), u.getId().equals(me),
                        stats.getOrDefault(u.getId(), WeekStats.EMPTY)))
                .sorted(ORDER)
                .toList();
        int[] ranks = ranks(rows);

        List<LeaderboardEntry> entries = new ArrayList<>(rows.size());
        int myRank = 0;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            UUID id = row.user().id();
            if (row.me()) {
                myRank = ranks[i];
            }
            entries.add(new LeaderboardEntry(ranks[i], row.user(), row.me(), row.stats(),
                    received.getOrDefault(id, ReactionCounts.NONE),
                    row.me() ? null : given.get(id)));
        }

        LocalDate firstActive = days.firstActiveDay(ids);
        boolean canGoBack = weekOffset > -Weeks.MAX_WEEKS_BACK && firstActive != null && firstActive.isBefore(start);
        return new SocialDtos.Leaderboard(new SocialDtos.Week(weekOffset, start, end), canGoBack, weekOffset < 0,
                myRank, entries);
    }

    /** Un rand inainte de a primi locul in clasament. */
    record Row(UserSummary user, boolean me, WeekStats stats) {
    }

    private static final Collator NAMES = Collator.getInstance(Locale.forLanguageTag("ro"));

    /**
     * Ordinea din clasament:
     * 1. procentajul (cei fara raspunsuri la final);
     * 2. la egalitate, cine a raspuns la mai multe carduri;
     * 3. apoi cine a studiat mai mult timp;
     * 4. apoi alfabetic dupa nume.
     */
    static final Comparator<Row> ORDER = Comparator
            .comparing((Row r) -> r.stats().score(), Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(r -> r.stats().answered(), Comparator.reverseOrder())
            .thenComparing(r -> r.stats().studySeconds(), Comparator.reverseOrder())
            .thenComparing(r -> r.user().username(), NAMES)
            .thenComparing(r -> r.user().id());

    /** Locurile: cei cu acelasi procentaj au acelasi loc, urmatorul sare (1, 2, 2, 4). Randurile sunt deja sortate. */
    static int[] ranks(List<Row> sorted) {
        int[] ranks = new int[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            boolean tie = i > 0 && Objects.equals(sorted.get(i).stats().score(), sorted.get(i - 1).stats().score());
            ranks[i] = tie ? ranks[i - 1] : i + 1;
        }
        return ranks;
    }

    private Map<UUID, ReactionCounts> countReceived(List<UUID> userIds, LocalDate week) {
        Map<UUID, long[]> counts = new HashMap<>();
        for (Object[] row : reactions.countReceived(userIds, week)) {
            long[] c = counts.computeIfAbsent((UUID) row[0], k -> new long[2]);
            c[row[1] == ReactionType.LIKE ? 0 : 1] += num(row[2]);
        }
        Map<UUID, ReactionCounts> result = new HashMap<>();
        counts.forEach((id, c) -> result.put(id, new ReactionCounts(c[0], c[1])));
        return result;
    }

    // ------------------------------------------------------------------ reactii

    /**
     * Seteaza like / dislike pentru procentajul unui prieten dintr-o saptamana.
     * Daca aveai deja cealalta reactie, e inlocuita. Returneaza randul actualizat al prietenului.
     */
    @Transactional
    public LeaderboardEntry react(UUID me, UUID friendId, int weekOffset, SocialDtos.ReactRequest request) {
        ReactionType type = ReactionType.parse(request == null ? null : request.type());
        LocalDate week = Weeks.start(today(), weekOffset);
        friendService.requireFriends(me, friendId);

        FriendReaction existing = reactions.findOne(me, friendId, week).orElse(null);
        if (existing != null) {
            existing.change(type);
        } else {
            try {
                reactions.saveAndFlush(new FriendReaction(users.getReferenceById(me),
                        users.getReferenceById(friendId), week, type));
            } catch (DataIntegrityViolationException e) {
                // doua click-uri simultane: prima reactie a castigat
                throw new ApiException(HttpStatus.CONFLICT, "Reacția a fost deja salvată. Reîncarcă clasamentul.");
            }
        }
        return entryFor(me, friendId, weekOffset);
    }

    /** Retrage reactia ta (al doilea click pe acelasi buton). */
    @Transactional
    public LeaderboardEntry removeReaction(UUID me, UUID friendId, int weekOffset) {
        LocalDate week = Weeks.start(today(), weekOffset);
        friendService.requireFriends(me, friendId);
        reactions.findOne(me, friendId, week).ifPresent(reactions::delete);
        reactions.flush();
        return entryFor(me, friendId, weekOffset);
    }

    /** Cine ti-a dat like / dislike (cele mai recente primele). */
    @Transactional(readOnly = true)
    public List<SocialDtos.ReceivedReaction> received(UUID me) {
        return reactions.findReceived(me).stream()
                .map(r -> new SocialDtos.ReceivedReaction(UserSummary.of(r.getFromUser()), r.getType(),
                        r.getWeekStart(), r.getUpdatedAt()))
                .toList();
    }

    private LeaderboardEntry entryFor(UUID me, UUID friendId, int weekOffset) {
        return leaderboard(me, weekOffset).entries().stream()
                .filter(e -> e.user().id().equals(friendId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Poți reacționa doar la prietenii tăi."));
    }

    private LocalDate today() {
        return LocalDate.now(properties.zoneId());
    }

    private static long num(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }
}
