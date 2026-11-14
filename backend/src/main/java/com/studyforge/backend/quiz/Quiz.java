package com.studyforge.backend.quiz;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.studyforge.backend.auth.AppUser;
import com.studyforge.backend.common.BaseEntity;
import com.studyforge.backend.common.FileSupport;
import com.studyforge.backend.plan.LearningPlan;
import com.studyforge.backend.storage.StoredPdf;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Un set de flashcard-uri generat pentru o saptamana dintr-un plan.
 */
@Entity
@Table(name = "quiz")
public class Quiz extends BaseEntity {

    /** Poate fi null daca PDF-ul incarcat nu a fost recunoscut ca plan StudyForge. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_plan_id")
    private LearningPlan learningPlan;

    /** Ex.: "Săptămâna 3 · Integrale definite" */
    @Column(name = "learning_stage", nullable = false)
    private String learningStage;

    @Column(name = "week_number")
    private Integer weekNumber;

    @Column(name = "source_file_name")
    private String sourceFileName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pdf_id")
    private StoredPdf pdf;

    /** Utilizatorul caruia ii apartine setul. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    @Column(name = "created_at", columnDefinition = "timestamptz")
    private Instant createdAt;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Flashcard> flashcards = new ArrayList<>();

    protected Quiz() {
    }

    public Quiz(AppUser owner, LearningPlan learningPlan, String learningStage, Integer weekNumber,
            String sourceFileName) {
        this.owner = owner;
        this.learningPlan = learningPlan;
        this.learningStage = FileSupport.truncate(learningStage, 255);
        this.weekNumber = weekNumber;
        this.sourceFileName = FileSupport.truncate(sourceFileName, 255);
        this.createdAt = Instant.now();
    }

    public void addFlashcard(String question, String answer) {
        flashcards.add(new Flashcard(this, question, answer, flashcards.size()));
    }

    public void attachPdf(StoredPdf pdf) {
        this.pdf = pdf;
    }

    public AppUser getOwner() {
        return owner;
    }

    public LearningPlan getLearningPlan() {
        return learningPlan;
    }

    public String getLearningStage() {
        return learningStage;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public StoredPdf getPdf() {
        return pdf;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Flashcard> getFlashcards() {
        return flashcards;
    }
}
