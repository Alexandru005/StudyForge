package com.studyforge.backend.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.studyforge.backend.auth.Gender;
import com.studyforge.backend.common.ApiException;
import com.studyforge.backend.progress.WeekStats;
import com.studyforge.backend.social.LeaderboardService.Row;
import com.studyforge.backend.social.SocialDtos.UserSummary;

/** Ordinea si locurile din clasamentul saptamanal. Nu au nevoie de baza de date. */
class LeaderboardOrderTest {

    private static Row row(String name, int correct, int wrong, long seconds) {
        UserSummary user = new UserSummary(UUID.randomUUID(), name, "x@exemplu.ro", Gender.OTHER);
        return new Row(user, false, WeekStats.of(correct, wrong, seconds));
    }

    private static List<Row> sorted(Row... rows) {
        return List.of(rows).stream().sorted(LeaderboardService.ORDER).toList();
    }

    private static List<String> names(List<Row> rows) {
        return rows.stream().map(r -> r.user().username()).toList();
    }

    @Test
    void higherPercentFirstAndNoAnswersLast() {
        List<Row> rows = sorted(
                row("Vlad Stan", 0, 0, 0),
                row("Ema Rusu", 21, 13, 6600),   // 62%
                row("Dan Ionescu", 38, 4, 9600), // 90%
                row("Ana Pop", 42, 9, 11100));   // 82%
        assertThat(names(rows)).containsExactly("Dan Ionescu", "Ana Pop", "Ema Rusu", "Vlad Stan");
        assertThat(LeaderboardService.ranks(rows)).containsExactly(1, 2, 3, 4);
    }

    @Test
    void zeroPercentStillBeatsNoAnswers() {
        assertThat(names(sorted(row("Nimic A", 0, 0, 500), row("Zero B", 0, 4, 0))))
                .containsExactly("Zero B", "Nimic A");
    }

    @Test
    void samePercentSharesRankAndIsOrderedByAnsweredThenTime() {
        List<Row> rows = sorted(
                row("Zeta Z", 4, 1, 100),    // 80%, 5 raspunsuri
                row("Beta B", 8, 2, 100),    // 80%, 10 raspunsuri
                row("Alfa A", 8, 2, 900),    // 80%, 10 raspunsuri, mai mult timp
                row("Primul P", 9, 0, 0),    // 100%
                row("Ultimul U", 1, 1, 0));  // 50%
        assertThat(names(rows)).containsExactly("Primul P", "Alfa A", "Beta B", "Zeta Z", "Ultimul U");
        assertThat(LeaderboardService.ranks(rows)).containsExactly(1, 2, 2, 2, 5);
    }

    @Test
    void namesUseRomanianAlphabet() {
        assertThat(names(sorted(row("Ștefan B", 1, 0, 0), row("Sorin A", 1, 0, 0), row("Tudor C", 1, 0, 0))))
                .containsExactly("Sorin A", "Ștefan B", "Tudor C");
    }

    @Test
    void reactionTypeParsing() {
        assertThat(ReactionType.parse("LIKE")).isEqualTo(ReactionType.LIKE);
        assertThat(ReactionType.parse(" dislike ")).isEqualTo(ReactionType.DISLIKE);

        assertThatThrownBy(() -> ReactionType.parse("super")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> ReactionType.parse(null)).isInstanceOf(ApiException.class);
    }

    @Test
    void searchEmailIsNormalized() {
        assertThat(FriendService.normalizeEmail("  Ana.Pop@Exemplu.RO ")).isEqualTo("ana.pop@exemplu.ro");

        assertThatThrownBy(() -> FriendService.normalizeEmail("")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> FriendService.normalizeEmail("ana")).isInstanceOf(ApiException.class);
    }

    @Test
    void relationWithoutLinkIsNone() {
        assertThat(FriendService.relation(UUID.randomUUID(), null)).isEqualTo(SocialDtos.Relation.NONE);
    }
}
