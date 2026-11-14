package com.studyforge.backend.plan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Toate cautarile sunt filtrate dupa proprietar: un utilizator vede doar planurile lui.
 */
public interface LearningPlanRepository extends JpaRepository<LearningPlan, UUID> {

    @Query("select p from LearningPlan p where p.owner.id = :ownerId order by p.createdAt desc")
    List<LearningPlan> findAllByOwner(@Param("ownerId") UUID ownerId);

    @Query("select p from LearningPlan p where p.id = :id and p.owner.id = :ownerId")
    Optional<LearningPlan> findByIdAndOwner(@Param("id") UUID id, @Param("ownerId") UUID ownerId);

    /** Planul al carui PDF are exact acest hash (utilizatorul reincarca PDF-ul descarcat). */
    @Query("select p from LearningPlan p where p.pdf.sha256 = :sha256 and p.owner.id = :ownerId order by p.createdAt desc")
    List<LearningPlan> findByPdfSha256AndOwner(@Param("sha256") String sha256, @Param("ownerId") UUID ownerId);

    default Optional<LearningPlan> findFirstByPdfSha256AndOwner(String sha256, UUID ownerId) {
        return findByPdfSha256AndOwner(sha256, ownerId).stream().findFirst();
    }
}
