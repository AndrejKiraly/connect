package com.andrejKir.connect.accounts;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
public class AppUserSettings {

    @Id
    private UUID appUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SupportedLanguage language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PostLifespan defaultPostLifespan;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
