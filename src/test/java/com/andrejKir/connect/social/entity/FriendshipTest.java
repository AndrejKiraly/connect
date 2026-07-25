package com.andrejKir.connect.social.entity;

import com.andrejKir.connect.shared.domain.UserPair;
import com.andrejKir.connect.social.enums.FriendshipStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FriendshipTest {

    private static final UUID REQUESTER_ID = UUID.randomUUID();
    private static final UUID TARGET_ID = UUID.randomUUID();

    @Test
    void request_startsPending() {
        assertEquals(FriendshipStatus.PENDING, pendingRequest().getStatus());
    }

    @Test
    void accept_whenAlreadyAccepted_isRejected() {
        Friendship friendship = pendingRequest();
        friendship.accept();

        assertThrows(IllegalStateException.class, friendship::accept);
    }

    @Test
    void decline_whenAlreadyAccepted_isRejected() {
        Friendship friendship = pendingRequest();
        friendship.accept();

        assertThrows(IllegalStateException.class, friendship::decline);
    }

    @Test
    void counterpartOf_uninvolvedUser_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> pendingRequest().counterpartOf(UUID.randomUUID()));
    }

    private static Friendship pendingRequest() {
        return Friendship.request(UserPair.of(REQUESTER_ID, TARGET_ID), REQUESTER_ID);
    }
}