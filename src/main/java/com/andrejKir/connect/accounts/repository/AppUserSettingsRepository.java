package com.andrejKir.connect.accounts.repository;

import com.andrejKir.connect.accounts.entity.AppUserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface AppUserSettingsRepository extends JpaRepository<AppUserSettings, UUID> {
}