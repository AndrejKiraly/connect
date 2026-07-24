package com.andrejKir.connect.social.repository;


import com.andrejKir.connect.shared.domain.UserPair;
import com.andrejKir.connect.social.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {


    Optional<Friendship> findByUserLowIdAndUserHighId(UUID userLowId, UUID userHighId);

    default Optional<Friendship> findByUsers(UserPair pair) {
        return findByUserLowIdAndUserHighId(pair.low(), pair.high());
    }

    @Query("""
           select f from Friendship f where f.status = com.andrejKir.connect.social.enums.FriendshipStatus.PENDING and f.requestedBy = :userId
                      """)
    List<Friendship> findPendingRequestsRequestedByUser(@Param("userId") UUID userId);

    @Query("""
           select f from Friendship f where f.status = com.andrejKir.connect.social.enums.FriendshipStatus.PENDING and f.requestedBy != :userId
           and (f.userLowId = :userId or f.userHighId = :userId)
""")
    List<Friendship> findPendingRequestsRequestedToUser(@Param("userId") UUID userId);

    @Query("""
           select f from Friendship f where f.status = com.andrejKir.connect.social.enums.FriendshipStatus.ACCEPTED
                      and (f.userHighId = :userId or f.userLowId = :userId) order by f.updatedAt desc
           """)
    List<Friendship> findAcceptedRequestsByUser(@Param("userId") UUID userId);


}
