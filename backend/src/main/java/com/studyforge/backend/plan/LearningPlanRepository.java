package com.studyforge.backend.plan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LearningPlanRepository extends JpaRepository<LearningPlan, UUID> {

    List<LearningPlan> findAllByOrderByCreatedAtDesc();

    /** Gaseste planul al carui PDF are exact acest hash (cazul in care utilizatorul reincarca PDF-ul descarcat). */
    @Query("select p from LearningPlan p where p.pdf.sha256 = :sha256 order by p.createdAt desc")
    List<LearningPlan> findByPdfSha256(@Param("sha256") String sha256);

    default Optional<LearningPlan> findFirstByPdfSha256(String sha256) {
        return findByPdfSha256(sha256).stream().findFirst();
    }
}
