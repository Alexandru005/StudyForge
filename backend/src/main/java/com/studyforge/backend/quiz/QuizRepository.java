package com.studyforge.backend.quiz;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Toate cautarile sunt filtrate dupa proprietar: un utilizator vede doar seturile lui.
 */
public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    @Query("select q from Quiz q left join fetch q.learningPlan where q.owner.id = :ownerId order by q.createdAt desc")
    List<Quiz> findAllByOwner(@Param("ownerId") UUID ownerId);

    @Query("""
            select q from Quiz q left join fetch q.learningPlan
            where q.owner.id = :ownerId and q.learningPlan.id = :planId
            order by q.createdAt desc""")
    List<Quiz> findAllByOwnerAndPlan(@Param("ownerId") UUID ownerId, @Param("planId") UUID planId);

    @Query("""
            select distinct q from Quiz q left join fetch q.learningPlan left join fetch q.flashcards
            where q.id = :id and q.owner.id = :ownerId""")
    Optional<Quiz> findWithFlashcards(@Param("id") UUID id, @Param("ownerId") UUID ownerId);

    @Query("select q from Quiz q where q.id = :id and q.owner.id = :ownerId")
    Optional<Quiz> findByIdAndOwner(@Param("id") UUID id, @Param("ownerId") UUID ownerId);

    /** Numarul de flashcard-uri per set al utilizatorului: randuri [quizId, count]. */
    @Query("select f.quiz.id, count(f) from Flashcard f where f.quiz.owner.id = :ownerId group by f.quiz.id")
    List<Object[]> countFlashcardsPerQuiz(@Param("ownerId") UUID ownerId);
}
