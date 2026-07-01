package com.andrejKir.connect.accounts.service;

import com.andrejKir.connect.accounts.dto.request.LoginRequest;
import com.andrejKir.connect.accounts.dto.request.RegisterRequest;
import com.andrejKir.connect.accounts.dto.response.AppUserResponse;
import com.andrejKir.connect.accounts.entity.AppUser;
import com.andrejKir.connect.accounts.exception.DuplicateEmailException;
import com.andrejKir.connect.accounts.exception.DuplicateUsernameException;
import com.andrejKir.connect.accounts.exception.InvalidCredentialsException;
import com.andrejKir.connect.accounts.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
                request.firstName() + " " + request.lastName(),
                request.firstName(),
                request.lastName(),
                request.birthDate()
                );
        appUserRepository.save(appUser);
        return  AppUserResponse.from(appUser);
    }

    public AppUserResponse loginUser(LoginRequest request) {
        AppUser appUser = appUserRepository.findByUsername(request.username())
                .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        return AppUserResponse.from(appUser);
    }
}