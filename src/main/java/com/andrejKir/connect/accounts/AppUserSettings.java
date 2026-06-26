package com.andrejKir.connect.accounts;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

public class AppUserSettings {

    @Id
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SupportedLanguage locale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PostLifespan defaultPostLifespan;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
