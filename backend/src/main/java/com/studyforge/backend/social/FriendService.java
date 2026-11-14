package com.studyforge.backend.social;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studyforge.backend.auth.AppUser;
import com.studyforge.backend.auth.AppUserRepository;
import com.studyforge.backend.common.ApiException;
import com.studyforge.backend.social.SocialDtos.FriendRequest;
import com.studyforge.backend.social.SocialDtos.Relation;
import com.studyforge.backend.social.SocialDtos.UserSummary;

/**
 * Cautare dupa email, cereri de prietenie si lista de prieteni.
 * Cautarea cere emailul complet (nu se cauta dupa bucati), ca sa nu se poata lista conturile altora.
 */
@Service
public class FriendService {

    private static final Logger log = LoggerFactory.getLogger(FriendService.class);

    private final AppUserRepository users;
    private final FriendshipRepository friendships;
    private final FriendReactionRepository reactions;

    public FriendService(AppUserRepository users, FriendshipRepository friendships,
            FriendReactionRepository reactions) {
        this.users = users;
        this.friendships = friendships;
        this.reactions = reactions;
    }

    // ------------------------------------------------------------------ cautare

    @Transactional(readOnly = true)
    public SocialDtos.SearchResult search(UUID me, String rawEmail) {
        AppUser found = findByEmail(rawEmail);
        if (found.getId().equals(me)) {
            return new SocialDtos.SearchResult(UserSummary.of(found), Relation.SELF, null);
        }
        Friendship link = friendships.findBetween(me, found.getId()).orElse(null);
        return new SocialDtos.SearchResult(UserSummary.of(found), relation(me, link),
                link != null && !link.isAccepted() ? link.getId() : null);
    }

    // ------------------------------------------------------------------ cereri

    @Transactional
    public SocialDtos.SendRequestResult sendRequest(UUID me, SocialDtos.SendRequest request) {
        AppUser target = findByEmail(request == null ? null : request.email());
        if (target.getId().equals(me)) {
            throw ApiException.badRequest("Nu te poți adăuga singur ca prieten.");
        }
        Friendship existing = friendships.findBetween(me, target.getId()).orElse(null);
        if (existing != null) {
            if (existing.isAccepted()) {
                throw new ApiException(HttpStatus.CONFLICT, "Sunteți deja prieteni.");
            }
            if (existing.getRequester().getId().equals(me)) {
                throw new ApiException(HttpStatus.CONFLICT, "I-ai trimis deja o cerere de prietenie.");
            }
            // Celalalt ti-a trimis deja o cerere: adaugarea lui inseamna acceptarea ei.
            existing.accept();
            return new SocialDtos.SendRequestResult(Relation.FRIENDS, toRequest(existing, target));
        }

        Friendship created = new Friendship(currentUser(me), target);
        try {
            friendships.saveAndFlush(created);
        } catch (DataIntegrityViolationException e) {
            // Doi utilizatori si-au trimis cereri in acelasi timp: indexul unic pe pereche o prinde.
            throw new ApiException(HttpStatus.CONFLICT, "Există deja o cerere de prietenie între voi. Reîncarcă pagina.");
        }
        log.info("Cerere de prietenie {} -> {}", me, target.getId());
        return new SocialDtos.SendRequestResult(Relation.REQUEST_SENT, toRequest(created, target));
    }

    @Transactional(readOnly = true)
    public SocialDtos.FriendRequests requests(UUID me) {
        List<FriendRequest> incoming = friendships.findIncoming(me, FriendshipStatus.PENDING).stream()
                .map(f -> toRequest(f, f.getRequester()))
                .toList();
        List<FriendRequest> outgoing = friendships.findOutgoing(me, FriendshipStatus.PENDING).stream()
                .map(f -> toRequest(f, f.getAddressee()))
                .toList();
        return new SocialDtos.FriendRequests(incoming, outgoing);
    }

    /** Doar destinatarul poate accepta. */
    @Transactional
    public SocialDtos.Friend accept(UUID me, UUID requestId) {
        Friendship request = friendships.findById(requestId)
                .filter(f -> !f.isAccepted() && f.getAddressee().getId().equals(me))
                .orElseThrow(() -> ApiException.notFound("Cererea de prietenie nu există sau a fost deja rezolvată."));
        request.accept();
        AppUser friend = request.getRequester();
        return new SocialDtos.Friend(UserSummary.of(friend), request.getAcceptedAt());
    }

    /** Destinatarul refuza cererea sau expeditorul o anuleaza. */
    @Transactional
    public void deleteRequest(UUID me, UUID requestId) {
        Friendship request = friendships.findById(requestId)
                .filter(f -> !f.isAccepted())
                .filter(f -> f.getRequester().getId().equals(me) || f.getAddressee().getId().equals(me))
                .orElseThrow(() -> ApiException.notFound("Cererea de prietenie nu există sau a fost deja rezolvată."));
        friendships.delete(request);
    }

    // ------------------------------------------------------------------ prieteni

    @Transactional(readOnly = true)
    public List<SocialDtos.Friend> friends(UUID me) {
        return friendships.findAllOfUser(me, FriendshipStatus.ACCEPTED).stream()
                .map(f -> {
                    AppUser friend = f.other(me);
                    return new SocialDtos.Friend(UserSummary.of(friend), f.getAcceptedAt());
                })
                .sorted(Comparator.comparing((SocialDtos.Friend f) -> f.user().username(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Sterge prietenia si reactiile dintre cei doi. */
    @Transactional
    public void removeFriend(UUID me, UUID friendId) {
        Friendship friendship = friendships.findBetween(me, friendId)
                .filter(Friendship::isAccepted)
                .orElseThrow(() -> ApiException.notFound("Nu sunteți prieteni."));
        reactions.deleteBetween(me, friendId);
        friendships.delete(friendship);
    }

    /** Arunca 404 daca cei doi nu sunt prieteni (folosit la reactii). */
    @Transactional(readOnly = true)
    public void requireFriends(UUID me, UUID otherId) {
        if (me.equals(otherId)) {
            throw ApiException.badRequest("Nu îți poți da singur o reacție.");
        }
        boolean friends = friendships.findBetween(me, otherId).map(Friendship::isAccepted).orElse(false);
        if (!friends) {
            throw ApiException.notFound("Poți reacționa doar la prietenii tăi.");
        }
    }

    // ------------------------------------------------------------------ utilitare

    private AppUser findByEmail(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        return users.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Nu există niciun cont cu acest email."));
    }

    private AppUser currentUser(UUID me) {
        return users.findById(me)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "Contul nu mai există. Autentifică-te din nou."));
    }

    /** Emailurile sunt salvate cu litere mici (vezi AuthService). */
    static String normalizeEmail(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.badRequest("Scrie adresa de email a prietenului.");
        }
        String email = raw.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 255 || !email.contains("@")) {
            throw ApiException.badRequest("Adresa de email nu are un format valid.");
        }
        return email;
    }

    static Relation relation(UUID me, Friendship link) {
        if (link == null) {
            return Relation.NONE;
        }
        if (link.isAccepted()) {
            return Relation.FRIENDS;
        }
        return link.getRequester().getId().equals(me) ? Relation.REQUEST_SENT : Relation.REQUEST_RECEIVED;
    }

    private static FriendRequest toRequest(Friendship f, AppUser other) {
        return new FriendRequest(f.getId(), UserSummary.of(other), f.getCreatedAt());
    }
}
