package com.studyforge.backend.quiz;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {

    /** Intrebarea, raspunsul si contextul unui flashcard, doar daca apartine utilizatorului. */
    @Query("""
            select new com.studyforge.backend.quiz.HintContext(f.question, f.answer, q.learningStage, p.subject)
            from Flashcard f
            join f.quiz q
            left join q.learningPlan p
            where f.id = :id and q.owner.id = :ownerId""")
    Optional<HintContext> findHintContext(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}