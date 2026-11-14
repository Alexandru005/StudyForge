package com.studyforge.backend.social;

import java.time.Instant;
import java.time.LocalDate;

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
 * Like / dislike dat de un utilizator procentajului unui prieten, pentru o saptamana din clasament.
 * Cel mult una per (from, to, saptamana): apasarea celuilalt buton o inlocuieste.
 */
@Entity
@Table(name = "friend_reaction")
public class FriendReaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_user_id", nullable = false)
    private AppUser fromUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_user_id", nullable = false)
    private AppUser toUser;

    /** Lunea saptamanii din clasament la care se refera reactia. */
    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10, columnDefinition = "varchar(10)")
    private ReactionType reactionType;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;

    protected FriendReaction() {
    }

    public FriendReaction(AppUser fromUser, AppUser toUser, LocalDate weekStart, ReactionType type) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.weekStart = weekStart;
        this.reactionType = type;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void change(ReactionType newType) {
        if (this.reactionType != newType) {
            this.reactionType = newType;
            this.updatedAt = Instant.now();
        }
    }

    public AppUser getFromUser() {
        return fromUser;
    }

    public AppUser getToUser() {
        return toUser;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public ReactionType getType() {
        return reactionType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
