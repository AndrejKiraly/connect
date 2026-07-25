package com.andrejKir.connect.shared.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserPairTest {

    private static final UUID LOWER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID HIGHER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void of_ordersPairCanonically_regardlessOfArgumentOrder() {
        assertEquals(UserPair.of(LOWER, HIGHER), UserPair.of(HIGHER, LOWER));
    }

    @Test
    void of_putsSmallerIdFirst() {
        UserPair pair = UserPair.of(HIGHER, LOWER);

        assertEquals(LOWER, pair.low());
        assertEquals(HIGHER, pair.high());
    }
}