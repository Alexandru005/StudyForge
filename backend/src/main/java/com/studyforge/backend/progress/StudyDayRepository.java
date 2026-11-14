package com.studyforge.backend.progress;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyDayRepository extends JpaRepository<StudyDay, UUID> {

    @Query("select d from StudyDay d where d.user.id = :userId and d.studyDate in :dates")
    List<StudyDay> findDays(@Param("userId") UUID userId, @Param("dates") Collection<LocalDate> dates);

    /** Totalurile pe un interval, per utilizator: randuri [userId, sum(correct), sum(wrong), sum(activeSeconds)]. */
    @Query("""
            select d.user.id, sum(d.correct), sum(d.wrong), sum(d.activeSeconds) from StudyDay d
            where d.user.id in :userIds and d.studyDate between :from and :to
            group by d.user.id""")
    List<Object[]> totals(@Param("userIds") Collection<UUID> userIds, @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Prima zi cu activitate a oricaruia dintre utilizatori (null daca nu exista). */
    @Query("""
            select min(d.studyDate) from StudyDay d
            where d.user.id in :userIds and (d.correct > 0 or d.wrong > 0 or d.activeSeconds > 0)""")
    LocalDate firstActiveDay(@Param("userIds") Collection<UUID> userIds);
}
