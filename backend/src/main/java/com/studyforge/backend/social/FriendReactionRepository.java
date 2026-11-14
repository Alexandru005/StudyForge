package com.studyforge.backend.social;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendReactionRepository extends JpaRepository<FriendReaction, UUID> {

    @Query("""
            select r from FriendReaction r
            where r.fromUser.id = :fromId and r.toUser.id = :toId and r.weekStart = :week""")
    Optional<FriendReaction> findOne(@Param("fromId") UUID fromId, @Param("toId") UUID toId,
            @Param("week") LocalDate week);

    /** Cate reactii de fiecare tip a primit fiecare utilizator intr-o saptamana: randuri [toUserId, tip, count]. */
    @Query("""
            select r.toUser.id, r.reactionType, count(r) from FriendReaction r
            where r.toUser.id in :userIds and r.weekStart = :week
            group by r.toUser.id, r.reactionType""")
    List<Object[]> countReceived(@Param("userIds") Collection<UUID> userIds, @Param("week") LocalDate week);

    /** Reactiile date de un utilizator intr-o saptamana: randuri [toUserId, tip]. */
    @Query("select r.toUser.id, r.reactionType from FriendReaction r where r.fromUser.id = :fromId and r.weekStart = :week")
    List<Object[]> findGivenBy(@Param("fromId") UUID fromId, @Param("week") LocalDate week);

    /** Reactiile primite de un utilizator, cu cine le-a dat, cele mai recente primele. */
    @Query("""
            select r from FriendReaction r join fetch r.fromUser
            where r.toUser.id = :toId
            order by r.updatedAt desc""")
    List<FriendReaction> findReceived(@Param("toId") UUID toId);

    /** La stergerea unei prietenii dispar si reactiile dintre cei doi (din toate saptamanile). */
    @Modifying
    @Query("""
            delete from FriendReaction r
            where (r.fromUser.id = :a and r.toUser.id = :b)
               or (r.fromUser.id = :b and r.toUser.id = :a)""")
    int deleteBetween(@Param("a") UUID a, @Param("b") UUID b);
}
