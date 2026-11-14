package com.studyforge.backend.social;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    /** Randul dintre doi utilizatori, indiferent cine a trimis cererea. */
    @Query("""
            select f from Friendship f
            where (f.requester.id = :a and f.addressee.id = :b)
               or (f.requester.id = :b and f.addressee.id = :a)""")
    Optional<Friendship> findBetween(@Param("a") UUID a, @Param("b") UUID b);

    /** Prieteniile (ACCEPTED) ale unui utilizator, cu ambii utilizatori incarcati. */
    @Query("""
            select f from Friendship f join fetch f.requester join fetch f.addressee
            where f.status = :status and (f.requester.id = :userId or f.addressee.id = :userId)""")
    List<Friendship> findAllOfUser(@Param("userId") UUID userId, @Param("status") FriendshipStatus status);

    /** Cererile primite (PENDING), cele mai noi primele. */
    @Query("""
            select f from Friendship f join fetch f.requester
            where f.addressee.id = :userId and f.status = :status
            order by f.createdAt desc""")
    List<Friendship> findIncoming(@Param("userId") UUID userId, @Param("status") FriendshipStatus status);

    /** Cererile trimise (PENDING), cele mai noi primele. */
    @Query("""
            select f from Friendship f join fetch f.addressee
            where f.requester.id = :userId and f.status = :status
            order by f.createdAt desc""")
    List<Friendship> findOutgoing(@Param("userId") UUID userId, @Param("status") FriendshipStatus status);
}
