package com.andrejKir.connect.accounts.service;

import com.andrejKir.connect.accounts.dto.request.RegisterRequest;
import com.andrejKir.connect.accounts.dto.response.AppUserPrivateDetailResponse;
import com.andrejKir.connect.accounts.dto.response.AppUserPrivateSummaryResponse;
import com.andrejKir.connect.accounts.dto.response.AppUserPublicDetailResponse;
import com.andrejKir.connect.accounts.dto.response.AppUserPublicSummaryResponse;
import com.andrejKir.connect.accounts.entity.AppUser;
import com.andrejKir.connect.accounts.exception.AppUserNotFoundException;
import com.andrejKir.connect.accounts.exception.DuplicateEmailException;
import com.andrejKir.connect.accounts.exception.DuplicateUserException;
import com.andrejKir.connect.accounts.exception.DuplicateUsernameException;
import com.andrejKir.connect.accounts.repository.AppUserRepository;
import com.andrejKir.connect.shared.ratelimit.RateLimitPolicy;
import com.andrejKir.connect.shared.ratelimit.RateLimitService;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AppUserService {

    private static final int SEARCH_QUERY_MIN_LENGTH = 2;
    private static final int SEARCH_QUERY_MAX_LENGTH = 100;
    private static final int SEARCH_RESULT_LIMIT = 20;

    private final AppUserRepository appUserRepository;
    private final AppUserSettingsService appUserSettingsService;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final RateLimitService rateLimitService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AppUserService(AppUserRepository appUserRepository, AppUserSettingsService appUserSettingsService,
                          InviteCodeGenerator inviteCodeGenerator, RateLimitService rateLimitService,
                          PasswordEncoder passwordEncoder, Clock clock) {
        this.appUserRepository = appUserRepository;
        this.appUserSettingsService = appUserSettingsService;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.rateLimitService = rateLimitService;
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

    @Transactional(readOnly = true)
    public List<AppUserPublicSummaryResponse> searchUsersByDisplayName(String query, UUID actorId) {
        rateLimitService.check(RateLimitPolicy.USER_SEARCH_PER_USER, actorId.toString());

        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery.length() < SEARCH_QUERY_MIN_LENGTH) {
            return List.of();
        }
        return appUserRepository.searchDiscoverableByDisplayName(actorId, normalizedQuery, SEARCH_RESULT_LIMIT)
                .stream()
                .map(AppUserPublicSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppUserPublicSummaryResponse> findUserByInviteCode(String code, UUID actorId) {
        rateLimitService.check(RateLimitPolicy.USER_CODE_LOOKUP_PER_USER, actorId.toString());

        return appUserRepository.findDiscoverableByInviteCode(normalizeInviteCode(code))
                .stream()
                .map(AppUserPublicSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppUserPublicDetailResponse getPublicDetail(UUID id) {
        return appUserRepository.findById(id)
                .map(AppUserPublicDetailResponse::from)
                .orElseThrow(AppUserNotFoundException::new);
    }

    public boolean exists(UUID id){
        return appUserRepository.existsById(id);
    }

    private static String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        String trimmed = query.trim();
        return trimmed.length() > SEARCH_QUERY_MAX_LENGTH
                ? trimmed.substring(0, SEARCH_QUERY_MAX_LENGTH)
                : trimmed;
    }

    private static String normalizeInviteCode(String inviteCode) {
        return inviteCode == null
                ? ""
                : inviteCode.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
    }

    private boolean isInviteCodeCollision(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException violation
                && "uq_app_user_invite_code".equals(violation.getConstraintName());
    }

}