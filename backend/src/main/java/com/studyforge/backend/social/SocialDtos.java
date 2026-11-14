package com.studyforge.backend.social;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.studyforge.backend.auth.AppUser;
import com.studyforge.backend.auth.Gender;
import com.studyforge.backend.progress.WeekStats;

/** Corpurile JSON pentru /api/friends. */
public final class SocialDtos {

    private SocialDtos() {
    }

    /** Datele publice ale unui utilizator (gender alege avatarul). */
    public record UserSummary(UUID id, String username, String email, Gender gender) {

        public static UserSummary of(AppUser user) {
            return new UserSummary(user.getId(), user.getUsername(), user.getEmail(), user.getGender());
        }
    }

    /** Ce legatura ai cu un utilizator gasit la cautare. */
    public enum Relation {
        /** E chiar contul tau. */
        SELF,
        /** Nicio legatura: poti trimite cerere. */
        NONE,
        /** Ai trimis o cerere, inca neacceptata. */
        REQUEST_SENT,
        /** Ti-a trimis o cerere: o poti accepta. */
        REQUEST_RECEIVED,
        /** Sunteti prieteni. */
        FRIENDS
    }

    /** GET /api/friends/search?email=... ; requestId e completat cand exista o cerere in asteptare. */
    public record SearchResult(UserSummary user, Relation relation, UUID requestId) {
    }

    /** POST /api/friends/requests */
    public record SendRequest(String email) {
    }

    /** O cerere de prietenie; {@code user} e celalalt utilizator (expeditorul sau destinatarul). */
    public record FriendRequest(UUID id, UserSummary user, Instant createdAt) {
    }

    public record FriendRequests(List<FriendRequest> incoming, List<FriendRequest> outgoing) {
    }

    /**
     * Raspunsul la "Adauga". Daca celalalt iti trimisese deja o cerere,
     * ea se accepta automat si relation = FRIENDS.
     */
    public record SendRequestResult(Relation relation, FriendRequest request) {
    }

    public record Friend(UserSummary user, Instant friendsSince) {
    }

    /** Like-urile si dislike-urile primite intr-o saptamana. */
    public record ReactionCounts(long likes, long dislikes) {

        public static final ReactionCounts NONE = new ReactionCounts(0, 0);
    }

    /** PUT /api/friends/{friendId}/reaction?week=0 : { "type": "LIKE" } sau { "type": "DISLIKE" } */
    public record ReactRequest(String type) {
    }

    /**
     * Saptamana afisata: offset 0 = saptamana aceasta, -1 = trecuta ...
     * start = luni, end = duminica (inclusiv).
     */
    public record Week(int offset, LocalDate start, LocalDate end) {
    }

    /**
     * Un rand din clasament.
     * - rank: locul; cei cu acelasi procentaj au acelasi loc (ex. 1, 2, 2, 4)
     * - me: true pentru randul tau
     * - stats: corecte / gresite / procentaj / timp de studiu in saptamana respectiva
     * - reactions: like-urile si dislike-urile primite pentru saptamana respectiva
     * - myReaction: ce i-ai dat tu (null daca nimic; mereu null pe randul tau)
     */
    public record LeaderboardEntry(int rank, UserSummary user, boolean me, WeekStats stats,
            ReactionCounts reactions, ReactionType myReaction) {
    }

    /**
     * canGoBack: exista date mai vechi (butonul „‹”); canGoForward: nu e saptamana curenta (butonul „›”).
     */
    public record Leaderboard(Week week, boolean canGoBack, boolean canGoForward, int myRank,
            List<LeaderboardEntry> entries) {
    }

    /** O reactie primita: cine, ce, pentru ce saptamana si cand. */
    public record ReceivedReaction(UserSummary from, ReactionType type, LocalDate weekStart, Instant at) {
    }
}
