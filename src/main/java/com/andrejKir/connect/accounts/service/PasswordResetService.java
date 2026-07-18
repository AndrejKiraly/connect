package com.andrejKir.connect.accounts.service;


import com.andrejKir.connect.accounts.entity.AppUser;
import com.andrejKir.connect.accounts.entity.PasswordResetToken;
import com.andrejKir.connect.accounts.exception.InvalidPasswordResetTokenException;
import com.andrejKir.connect.accounts.repository.AppUserRepository;
import com.andrejKir.connect.accounts.repository.PasswordResetTokenRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PasswordResetService {

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int TOKEN_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();


    public PasswordResetService(PasswordResetTokenRepository passwordResetTokenRepository,
                                AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        this.sessionRepository = sessionRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<String> generatePasswordResetToken(String email) {
        Optional<AppUser> appUser = appUserRepository.findByEmail(email);
        if (appUser.isEmpty()){
            return Optional.empty();
        }
        String newToken = generatePlaintextToken();
        PasswordResetToken passwordResetToken = new PasswordResetToken(appUser.get().getId(), hashToken(newToken), Instant.now().plus(15, ChronoUnit.MINUTES));
        passwordResetTokenRepository.save(passwordResetToken);
        return Optional.of(newToken);
    }

    @Transactional
    public void resetPassword( String resetToken, String newPassword){
        Instant resetTime = Instant.now();
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hashToken(resetToken))
                .orElseThrow(InvalidPasswordResetTokenException::new);
        if (token.isUsed() || token.isExpired(resetTime)) {
            throw new InvalidPasswordResetTokenException();
        }
        AppUser user = appUserRepository.findById(token.getUserId())
                .orElseThrow(InvalidPasswordResetTokenException::new);
        token.markUsed(resetTime);
        user.changePassword(passwordEncoder.encode(newPassword));

        sessionRepository.findByPrincipalName(user.getUsername())
                .keySet()
                .forEach(sessionRepository::deleteById);
    }

    private String generatePlaintextToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String plaintext){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plaintext.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }catch (NoSuchAlgorithmException e){
            throw new IllegalStateException("SHA-256 not available", e);
        }

    }

}
