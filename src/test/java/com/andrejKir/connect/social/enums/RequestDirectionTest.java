package com.andrejKir.connect.social.enums;

import com.andrejKir.connect.social.exception.InvalidFriendshipDirectionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestDirectionTest {

    @ParameterizedTest
    @CsvSource({
            "incoming,INCOMING",
            "INCOMING,INCOMING",
            "Incoming,INCOMING",
            "outgoing,OUTGOING",
            "OUTGOING,OUTGOING"
    })
    void from_matchesRegardlessOfCase(String value, RequestDirection expected) {
        assertEquals(expected, RequestDirection.from(value));
    }

    @Test
    void from_missingValue_isRejected() {
        assertThrows(InvalidFriendshipDirectionException.class, () -> RequestDirection.from(null));
    }

    @Test
    void from_unknownValue_isRejected() {
        assertThrows(InvalidFriendshipDirectionException.class, () -> RequestDirection.from("sideways"));
    }
}