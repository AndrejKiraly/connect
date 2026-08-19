package com.andrejKir.connect.accounts.entity;

import com.andrejKir.connect.accounts.enums.PostLifespan;
import com.andrejKir.connect.accounts.enums.SupportedLanguage;
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

    @Column(nullable = false)
    private boolean discoverableByName;

    @Column(nullable = false)
    private boolean discoverableByCode;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected AppUserSettings() {
    }

    public AppUserSettings(UUID appUserId) {
        this.appUserId = appUserId;
        this.language = SupportedLanguage.EN;
        this.defaultPostLifespan = PostLifespan.FOREVER;
        this.discoverableByName = true;
        this.discoverableByCode = true;
    }

    public void update(SupportedLanguage language, PostLifespan defaultPostLifespan,
                       boolean discoverableByName, boolean discoverableByCode) {
        this.language = language;
        this.defaultPostLifespan = defaultPostLifespan;
        this.discoverableByName = discoverableByName;
        this.discoverableByCode = discoverableByCode;
    }

    public UUID getAppUserId() {
        return appUserId;
    }

    public SupportedLanguage getLanguage() {
        return language;
    }

    public PostLifespan getDefaultPostLifespan() {
        return defaultPostLifespan;
    }

    public boolean isDiscoverableByName() {
        return discoverableByName;
    }

    public boolean isDiscoverableByCode() {
        return discoverableByCode;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
