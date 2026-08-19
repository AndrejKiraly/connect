package com.andrejKir.connect.accounts.repository;

import com.andrejKir.connect.accounts.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface AppUserRepository extends JpaRepository< AppUser, UUID> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByInviteCode(String inviteCode);

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

    @Query(value = """
           SELECT u.*
             FROM app_user u
             JOIN app_user_settings s ON s.app_user_id = u.id
            WHERE u.deactivated_at IS NULL
              AND s.discoverable_by_name
              AND u.id <> :actorId
              AND strpos(lower(unaccent(u.display_name)), lower(unaccent(:query))) > 0
            ORDER BY (strpos(lower(unaccent(u.display_name)), lower(unaccent(:query))) = 1) DESC,
                     u.display_name,
                     u.id
            LIMIT :limit
           """, nativeQuery = true)
    List<AppUser> searchDiscoverableByDisplayName(@Param("actorId") UUID actorId,
                                                  @Param("query") String query,
                                                  @Param("limit") int limit);

    // TODO: decide if we let actor find himself via code
    @Query("""
           select u from AppUser u, AppUserSettings s
            where s.appUserId = u.id
              and u.inviteCode = :inviteCode
              and u.deactivatedAt is null
              and s.discoverableByCode
           """)
    Optional<AppUser> findDiscoverableByInviteCode(@Param("inviteCode") String inviteCode);
}
