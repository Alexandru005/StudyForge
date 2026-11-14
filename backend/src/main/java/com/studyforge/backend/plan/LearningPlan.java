package com.studyforge.backend.plan;

import java.time.Instant;
import java.time.LocalDate;

import com.studyforge.backend.auth.AppUser;
import com.studyforge.backend.common.BaseEntity;
import com.studyforge.backend.common.FileSupport;
import com.studyforge.backend.storage.StoredPdf;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "learning_plan")
public class LearningPlan extends BaseEntity {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "subject")
    private String subject;

    @Column(name = "syllabus_file_name")
    private String syllabusFileName;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "week_count", nullable = false)
    private int weekCount;

    /** Structura completa a planului (PlanContent serializat), folosita la generarea flashcard-urilor. */
    @Column(name = "content_json", columnDefinition = "text")
    private String contentJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pdf_id")
    private StoredPdf pdf;

    /** Utilizatorul caruia ii apartine planul. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    @Column(name = "created_at", columnDefinition = "timestamptz")
    private Instant createdAt;

    protected LearningPlan() {
    }

    public LearningPlan(String title, String subject, String syllabusFileName, LocalDate startDate,
            LocalDate endDate, int weekCount, String contentJson) {
        this.title = FileSupport.truncate(title, 255);
        this.subject = FileSupport.truncate(subject, 255);
        this.syllabusFileName = FileSupport.truncate(syllabusFileName, 255);
        this.startDate = startDate;
        this.endDate = endDate;
        this.weekCount = weekCount;
        this.contentJson = contentJson;
        this.createdAt = Instant.now();
    }

    public void attachPdf(StoredPdf pdf) {
        this.pdf = pdf;
    }

    public void assignOwner(AppUser owner) {
        this.owner = owner;
    }

    public AppUser getOwner() {
        return owner;
    }

    public String getTitle() {
        return title;
    }

    public String getSubject() {
        return subject;
    }

    public String getSyllabusFileName() {
        return syllabusFileName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getWeekCount() {
        return weekCount;
    }

    public String getContentJson() {
        return contentJson;
    }

    public StoredPdf getPdf() {
        return pdf;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
