package com.nurba.java.config;

import com.nurba.java.domain.AppUser;
import com.nurba.java.enums.Role;
import com.nurba.java.repositories.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumSet;

@Component
@RequiredArgsConstructor
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.bootstrap-admin-email:}")
    private String bootstrapAdminEmail;

    @Value("${app.security.bootstrap-admin-password:}")
    private String bootstrapAdminPassword;

    @Override
    public void run(ApplicationArguments args) {
        String email = bootstrapAdminEmail != null ? bootstrapAdminEmail.trim().toLowerCase() : "";
        String password = bootstrapAdminPassword != null ? bootstrapAdminPassword : "";

        if (email.isEmpty() || password.isEmpty()) {
            return;
        }
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        AppUser admin = AppUser.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .roles(EnumSet.of(Role.ADMIN))
                .createdAt(Instant.now())
                .build();
        appUserRepository.save(admin);
    }
}
