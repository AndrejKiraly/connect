package com.andrejKir.connect.accounts.dto.response;

import com.andrejKir.connect.accounts.entity.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppUserPublicSummaryResponseTest {

    private static final String DISPLAY_NAME = "Lubo Ander";

    @Test
    void from_activeUser_exposesDisplayName() {
        AppUserPublicSummaryResponse response = AppUserPublicSummaryResponse.from(user());

        assertEquals(DISPLAY_NAME, response.displayName());
        assertFalse(response.deleted());
    }

    @Test
    void from_deactivatedUser_hidesDisplayName() {
        AppUser user = user();
        user.deactivate(Instant.now());

        AppUserPublicSummaryResponse response = AppUserPublicSummaryResponse.from(user);

        assertNotEquals(DISPLAY_NAME, response.displayName());
        assertTrue(response.deleted());
    }

    @Test
    void from_deactivatedUser_keepsId() {
        AppUser user = user();
        user.deactivate(Instant.now());

        assertEquals(user.getId(), AppUserPublicSummaryResponse.from(user).id());
    }

    private static AppUser user() {
        return new AppUser("lubo", "lubo@example.com", "hash", DISPLAY_NAME,
                "Lubo", "Ander", "ABCD1234", LocalDate.of(2000, 1, 1));
    }
}