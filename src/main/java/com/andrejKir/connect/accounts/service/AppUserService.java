package com.andrejKir.connect.accounts.service;

import com.andrejKir.connect.accounts.dto.request.RegisterRequest;
import com.andrejKir.connect.accounts.dto.response.AppUserResponse;
import com.andrejKir.connect.accounts.entity.AppUser;
import com.andrejKir.connect.accounts.exception.DuplicateEmailException;
import com.andrejKir.connect.accounts.exception.DuplicateUserException;
import com.andrejKir.connect.accounts.exception.DuplicateUsernameException;
import com.andrejKir.connect.accounts.repository.AppUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUserResponse registerUser(RegisterRequest request) {
        if (appUserRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        if (appUserRepository.existsByUsername(request.username())){
            throw new DuplicateUsernameException(request.username());
        }

        AppUser appUser = new AppUser(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.displayName(),
                request.firstName(),
                request.lastName(),
                request.birthDate()
                );
        try {
            appUserRepository.saveAndFlush(appUser);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateUserException();
        }
        return  AppUserResponse.from(appUser);
    }

    public AppUserResponse getById(UUID id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + id));
        return AppUserResponse.from(user);
    }
}