package com.andrejKir.connect.accounts.dto.response;

import com.andrejKir.connect.accounts.entity.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AppUserPublicSummaryResponseTest {

    private static final String DISPLAY_NAME = "Lubo Ander";

    @Test
    void from_activeUser_exposesDisplayName() {
        assertEquals(DISPLAY_NAME, AppUserPublicSummaryResponse.from(user()).displayName());
    }

    @Test
    void from_deactivatedUser_hidesDisplayName() {
        AppUser user = user();
        user.deactivate(Instant.now());

        assertNotEquals(DISPLAY_NAME, AppUserPublicSummaryResponse.from(user).displayName());
    }

    @Test
    void from_deactivatedUser_keepsId() {
        AppUser user = user();
        user.deactivate(Instant.now());

        assertEquals(user.getId(), AppUserPublicSummaryResponse.from(user).id());
    }

    private static AppUser user() {
        return new AppUser("lubo", "lubo@example.com", "hash", DISPLAY_NAME,
                "Lubo", "Ander", LocalDate.of(2000, 1, 1));
    }
}