package com.studyforge.backend.quiz;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    @Query("select q from Quiz q left join fetch q.learningPlan order by q.createdAt desc")
    List<Quiz> findAllWithPlan();

    @Query("select q from Quiz q left join fetch q.learningPlan where q.learningPlan.id = :planId order by q.createdAt desc")
    List<Quiz> findAllByPlanId(@Param("planId") UUID planId);

    @Query("select distinct q from Quiz q left join fetch q.learningPlan left join fetch q.flashcards where q.id = :id")
    Optional<Quiz> findWithFlashcards(@Param("id") UUID id);

    /** Numarul de flashcard-uri per set: randuri [quizId, count]. */
    @Query("select f.quiz.id, count(f) from Flashcard f group by f.quiz.id")
    List<Object[]> countFlashcardsPerQuiz();
}
