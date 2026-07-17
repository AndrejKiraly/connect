package com.andrejKir.connect.social.repository;


import com.andrejKir.connect.shared.domain.UserPair;
import com.andrejKir.connect.social.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {


    Optional<Friendship> findByUserLowIdAndUserHighId(UUID userLowId, UUID userHighId);

    default Optional<Friendship> findByUsers(UserPair pair) {
        return findByUserLowIdAndUserHighId(pair.low(), pair.high());
    }


}
