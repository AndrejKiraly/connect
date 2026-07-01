package com.andrejKir.connect.accounts.repository;

import com.andrejKir.connect.accounts.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface AppUserRepository extends JpaRepository< AppUser, UUID> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<AppUser> findByUsername(String username);
}
