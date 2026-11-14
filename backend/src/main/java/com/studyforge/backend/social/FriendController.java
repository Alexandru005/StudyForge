package com.studyforge.backend.social;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.studyforge.backend.auth.CurrentUser;

/**
 * Prieteni (cautare dupa email, cereri), clasamentul saptamanal cu prietenii si like / dislike.
 * Toate endpoint-urile cer token.
 */
@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friends;
    private final LeaderboardService leaderboard;

    public FriendController(FriendService friends, LeaderboardService leaderboard) {
        this.friends = friends;
        this.leaderboard = leaderboard;
    }

    // ------------------------------------------------------------------ prieteni

    /** GET /api/friends/search?email=ana@exemplu.ro -> utilizatorul + relatia cu tine (404 daca nu exista). */
    @GetMapping("/search")
    public SocialDtos.SearchResult search(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String email) {
        return friends.search(CurrentUser.id(jwt), email);
    }

    /** Lista prietenilor. */
    @GetMapping
    public List<SocialDtos.Friend> list(@AuthenticationPrincipal Jwt jwt) {
        return friends.friends(CurrentUser.id(jwt));
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID friendId) {
        friends.removeFriend(CurrentUser.id(jwt), friendId);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ cereri

    /** { "email": "ana@exemplu.ro" } */
    @PostMapping("/requests")
    public ResponseEntity<SocialDtos.SendRequestResult> sendRequest(@AuthenticationPrincipal Jwt jwt,
            @RequestBody SocialDtos.SendRequest request) {
        SocialDtos.SendRequestResult result = friends.sendRequest(CurrentUser.id(jwt), request);
        HttpStatus status = result.relation() == SocialDtos.Relation.FRIENDS ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result);
    }

    /** Cererile primite (incoming) si trimise (outgoing), inca neacceptate. */
    @GetMapping("/requests")
    public SocialDtos.FriendRequests requests(@AuthenticationPrincipal Jwt jwt) {
        return friends.requests(CurrentUser.id(jwt));
    }

    @PostMapping("/requests/{requestId}/accept")
    public SocialDtos.Friend accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
        return friends.accept(CurrentUser.id(jwt), requestId);
    }

    /** Refuza (daca ai primit-o) sau anuleaza (daca ai trimis-o) o cerere. */
    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<Void> deleteRequest(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
        friends.deleteRequest(CurrentUser.id(jwt), requestId);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ clasament + reactii

    /** GET /api/friends/leaderboard?week=0 (0 = saptamana aceasta, -1 = trecuta ...). */
    @GetMapping("/leaderboard")
    public SocialDtos.Leaderboard leaderboard(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "week", defaultValue = "0") int week) {
        return leaderboard.leaderboard(CurrentUser.id(jwt), week);
    }

    /** PUT /api/friends/{friendId}/reaction?week=0 cu { "type": "LIKE" } sau { "type": "DISLIKE" }. */
    @PutMapping("/{friendId}/reaction")
    public SocialDtos.LeaderboardEntry react(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID friendId,
            @RequestParam(name = "week", defaultValue = "0") int week, @RequestBody SocialDtos.ReactRequest request) {
        return leaderboard.react(CurrentUser.id(jwt), friendId, week, request);
    }

    /** Retrage like-ul / dislike-ul dat unui prieten pentru saptamana respectiva. */
    @DeleteMapping("/{friendId}/reaction")
    public SocialDtos.LeaderboardEntry removeReaction(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID friendId,
            @RequestParam(name = "week", defaultValue = "0") int week) {
        return leaderboard.removeReaction(CurrentUser.id(jwt), friendId, week);
    }

    /** Cine ti-a dat like / dislike. */
    @GetMapping("/reactions/received")
    public List<SocialDtos.ReceivedReaction> receivedReactions(@AuthenticationPrincipal Jwt jwt) {
        return leaderboard.received(CurrentUser.id(jwt));
    }
}
