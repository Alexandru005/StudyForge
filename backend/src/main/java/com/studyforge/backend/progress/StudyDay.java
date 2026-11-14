package com.studyforge.backend.progress;

import java.time.Instant;
import java.time.LocalDate;

import com.studyforge.backend.auth.AppUser;
import com.studyforge.backend.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Totalurile unei zile de studiu (toate materiile), ca in pagina "Progres":
 * raspunsuri "stiut" / "de repetat" la flashcards si secunde de studiu activ.
 */
@Entity
@Table(name = "study_day")
public class StudyDay extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "study_date", nullable = false)
    private LocalDate studyDate;

    @Column(name = "correct_count", nullable = false)
    private int correct;

    @Column(name = "wrong_count", nullable = false)
    private int wrong;

    @Column(name = "active_seconds", nullable = false)
    private int activeSeconds;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;

    protected StudyDay() {
    }

    public StudyDay(AppUser user, LocalDate studyDate) {
        this.user = user;
        this.studyDate = studyDate;
        this.updatedAt = Instant.now();
    }

    /** Inlocuieste totalurile zilei cu cele trimise de frontend. */
    public void update(int correct, int wrong, int activeSeconds) {
        if (this.correct != correct || this.wrong != wrong || this.activeSeconds != activeSeconds) {
            this.correct = correct;
            this.wrong = wrong;
            this.activeSeconds = activeSeconds;
            this.updatedAt = Instant.now();
        }
    }

    public LocalDate getStudyDate() {
        return studyDate;
    }

    public int getCorrect() {
        return correct;
    }

    public int getWrong() {
        return wrong;
    }

    public int getActiveSeconds() {
        return activeSeconds;
    }
}
