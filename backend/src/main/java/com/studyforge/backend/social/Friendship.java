package com.studyforge.backend.social;

import java.time.Instant;
import java.util.UUID;

import com.studyforge.backend.auth.AppUser;
import com.studyforge.backend.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * O cerere de prietenie (PENDING) sau o prietenie (ACCEPTED) intre doi utilizatori.
 * Exista cel mult un rand per pereche (vezi indexul uq_friendship_pair din V5).
 */
@Entity
@Table(name = "friendship")
public class Friendship extends BaseEntity {

    /** Cine a trimis cererea. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private AppUser requester;

    /** Cine a primit cererea (doar el o poate accepta). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addressee_id", nullable = false)
    private AppUser addressee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10, columnDefinition = "varchar(10)")
    private FriendshipStatus status;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "accepted_at", columnDefinition = "timestamptz")
    private Instant acceptedAt;

    protected Friendship() {
    }

    public Friendship(AppUser requester, AppUser addressee) {
        this.requester = requester;
        this.addressee = addressee;
        this.status = FriendshipStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void accept() {
        if (status != FriendshipStatus.ACCEPTED) {
            status = FriendshipStatus.ACCEPTED;
            acceptedAt = Instant.now();
        }
    }

    public boolean isAccepted() {
        return status == FriendshipStatus.ACCEPTED;
    }

    /** Celalalt utilizator din pereche, vazut de {@code me}. */
    public AppUser other(UUID me) {
        return requester.getId().equals(me) ? addressee : requester;
    }

    public AppUser getRequester() {
        return requester;
    }

    public AppUser getAddressee() {
        return addressee;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }
}
