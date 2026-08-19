package com.andrejKir.connect.accounts.service;

import com.andrejKir.connect.accounts.dto.request.RegisterRequest;
import com.andrejKir.connect.accounts.dto.response.AppUserPrivateDetailResponse;
import com.andrejKir.connect.accounts.dto.response.AppUserPrivateSummaryResponse;
import com.andrejKir.connect.accounts.dto.response.AppUserPublicSummaryResponse;
import com.andrejKir.connect.accounts.entity.AppUser;
import com.andrejKir.connect.accounts.exception.DuplicateEmailException;
import com.andrejKir.connect.accounts.exception.DuplicateUserException;
import com.andrejKir.connect.accounts.exception.DuplicateUsernameException;
import com.andrejKir.connect.accounts.repository.AppUserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final AppUserSettingsService appUserSettingsService;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AppUserService(AppUserRepository appUserRepository, AppUserSettingsService appUserSettingsService,
                          InviteCodeGenerator inviteCodeGenerator, PasswordEncoder passwordEncoder, Clock clock) {
        this.appUserRepository = appUserRepository;
        this.appUserSettingsService = appUserSettingsService;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public AppUserPrivateSummaryResponse registerUser(RegisterRequest request) {
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
                inviteCodeGenerator.generate(),
                request.birthDate()
                );
        try {
            appUserRepository.saveAndFlush(appUser);
        } catch (DataIntegrityViolationException e) {
            if (isInviteCodeCollision(e)) {
                throw e;
            }
            throw new DuplicateUserException();
        }
        appUserSettingsService.createDefaults(appUser.getId());
        return  AppUserPrivateSummaryResponse.from(appUser);
    }

    public AppUserPrivateSummaryResponse getPrivateSummary(UUID id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + id));
        return AppUserPrivateSummaryResponse.from(user);
    }

    public AppUserPrivateDetailResponse getPrivateDetail(UUID id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + id));
        return AppUserPrivateDetailResponse.from(user);
    }

    @Transactional(readOnly = true)
    public AppUserPublicSummaryResponse getSummary(UUID id) {
        return appUserRepository.findById(id)
                .map(AppUserPublicSummaryResponse::from)
                .orElseThrow(() -> new IllegalStateException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public Map<UUID, AppUserPublicSummaryResponse> getSummaries(Set<UUID> ids) {
        return appUserRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(AppUser::getId, AppUserPublicSummaryResponse::from));
    }

    @Transactional
    public void deactivate(UUID id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + id));
        user.deactivate(clock.instant());
    }

    public boolean exists(UUID id){
        return appUserRepository.existsById(id);
    }

    private boolean isInviteCodeCollision(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException violation
                && "uq_app_user_invite_code".equals(violation.getConstraintName());
    }
}