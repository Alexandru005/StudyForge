package com.studyforge.backend.quiz;

import com.studyforge.backend.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "flashcard")
public class Flashcard extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "question", nullable = false, columnDefinition = "text")
    private String question;

    @Column(name = "answer", nullable = false, columnDefinition = "text")
    private String answer;

    /** Pregatit pentru modul interactiv / bara de progres (dupa MVP). */
    @Column(name = "is_mastered", columnDefinition = "bool")
    private Boolean mastered = Boolean.FALSE;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected Flashcard() {
    }

    Flashcard(Quiz quiz, String question, String answer, int orderIndex) {
        this.quiz = quiz;
        this.question = question;
        this.answer = answer;
        this.orderIndex = orderIndex;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public boolean isMastered() {
        return Boolean.TRUE.equals(mastered);
    }

    public void setMastered(boolean mastered) {
        this.mastered = mastered;
    }

    public int getOrderIndex() {
        return orderIndex;
    }
}
