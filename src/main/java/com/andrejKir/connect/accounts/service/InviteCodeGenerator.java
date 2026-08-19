package com.andrejKir.connect.accounts.service;

import com.andrejKir.connect.accounts.repository.AppUserRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class InviteCodeGenerator {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int LENGTH = 8;
    private static final int MAX_ATTEMPTS = 5;

    private final AppUserRepository appUserRepository;
    private final SecureRandom random = new SecureRandom();

    public InviteCodeGenerator(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public String generate(){
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String inviteCode = randomCode();
            if (!appUserRepository.existsByInviteCode(inviteCode)) {
                return inviteCode;
            }
        }
        throw new IllegalStateException("Could not generate an unused invite code");
    }

    private String randomCode(){
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i<LENGTH; i++){
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
    return code.toString();
    }





}
