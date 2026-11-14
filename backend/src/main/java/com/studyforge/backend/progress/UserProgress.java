package com.studyforge.backend.progress;

import java.time.Instant;

import com.studyforge.backend.auth.AppUser;
import com.studyforge.backend.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Progresul complet al unui utilizator (JSON-ul din pagina "Progres"), salvat ca text. */
@Entity
@Table(name = "user_progress")
public class UserProgress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(name = "data", nullable = false, columnDefinition = "text")
    private String data;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;

    protected UserProgress() {
    }

    public UserProgress(AppUser user, String data) {
        this.user = user;
        this.data = data;
        this.updatedAt = Instant.now();
    }

    public void replace(String newData) {
        if (!newData.equals(this.data)) {
            this.data = newData;
            this.updatedAt = Instant.now();
        }
    }

    public String getData() {
        return data;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
