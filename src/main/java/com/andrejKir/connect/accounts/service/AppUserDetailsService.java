package com.andrejKir.connect.accounts.service;

import com.andrejKir.connect.accounts.entity.AppUser;
import com.andrejKir.connect.accounts.repository.AppUserRepository;
import com.andrejKir.connect.accounts.security.SecurityUser;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public AppUserDetailsService(AppUserRepository appUserRepository){
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        Optional<AppUser> found = usernameOrEmail.contains("@")
                ? appUserRepository.findByEmail(usernameOrEmail)
                : appUserRepository.findByUsername(usernameOrEmail);
        return found.map(SecurityUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));


    }

}
