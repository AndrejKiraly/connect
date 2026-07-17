package com.andrejKir.connect.accounts.security;

import com.andrejKir.connect.accounts.entity.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Adaptér {@link AppUser} -> Spring Security {@link UserDetails}.
 * Cez {@code @AuthenticationPrincipal} sa k nemu dostaneme v controlleroch
 * a z {@link #getId()} zoberieme identitu aktéra, nikdy nie z tela requestu.
 */
public class SecurityUser implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String username;
    private final String passwordHash;

    public SecurityUser(AppUser appUser) {
        this.id = appUser.getId();
        this.username = appUser.getUsername();
        this.passwordHash = appUser.getPasswordHash();
    }

    public UUID getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
