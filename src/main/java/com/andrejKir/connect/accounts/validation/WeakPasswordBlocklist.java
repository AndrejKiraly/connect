package com.andrejKir.connect.accounts.validation;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class WeakPasswordBlocklist {
    private final Set<String> weakPasswords;

    public WeakPasswordBlocklist(){
        this.weakPasswords = load();
    }


    private Set<String> load(){
        ClassPathResource resource = new ClassPathResource("security/common_passwords.txt");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))){
                return reader.lines()
                        .map(line -> line.strip().toLowerCase(Locale.ROOT))
                        .filter(line -> !line.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
        }catch (IOException e){
            throw new IllegalStateException("Cannot load weak-password blocklist", e);
        }
    }

    public boolean contains(String password) {
        return weakPasswords.contains(password.toLowerCase(Locale.ROOT));
    }
}
