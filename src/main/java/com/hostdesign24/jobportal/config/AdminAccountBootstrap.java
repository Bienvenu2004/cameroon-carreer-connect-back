package com.hostdesign24.jobportal.config;

import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.AuthProvider;
import com.hostdesign24.jobportal.model.enums.UserRole;
import com.hostdesign24.jobportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Locale;
import java.util.Optional;

/**
 * Guarantees an administrator account from ADMIN_EMAIL / ADMIN_PASSWORD.
 *
 * V2__create_admin_prosper could not do this reliably. Flyway runs before
 * Hibernate creates the schema, so on a fresh database V2 found no users table,
 * skipped itself, and was still recorded as applied: production ended up with no
 * administrator and no way to get one. A migration is also the wrong place for a
 * production password, since it ends up in the repository.
 *
 * This runs once the schema exists, on every start. It only acts when the
 * variables are set and the account is not already an active administrator, so
 * a password changed later through the application is never overwritten. Leave
 * the variables unset where no bootstrap is wanted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountBootstrap implements ApplicationRunner {

    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = adminEmail == null ? "" : adminEmail.trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty()) {
            return;
        }
        if (adminPassword == null || adminPassword.length() < MIN_PASSWORD_LENGTH) {
            log.warn("ADMIN_EMAIL is set but ADMIN_PASSWORD is missing or shorter than {} characters; "
                    + "no administrator was created.", MIN_PASSWORD_LENGTH);
            return;
        }

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getRole() == UserRole.SYSTEM_ADMIN && user.isActive() && !user.isDeleted()) {
                return; // already in place; its password is the owner's business now
            }
            // The address exists with another role, or was suspended or deleted:
            // the deployment says this address is the administrator, so make it one.
            user.setRole(UserRole.SYSTEM_ADMIN);
            user.setActive(true);
            user.setDeleted(false);
            user.setAuthProvider(AuthProvider.LOCAL);
            user.setPassword(passwordEncoder.encode(adminPassword));
            user.setPasswordChangedAt(new Date());
            userRepository.save(user);
            log.info("Administrator account ensured for {} (existing account promoted)", mask(email));
            return;
        }

        User admin = new User();
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRole.SYSTEM_ADMIN);
        admin.setAuthProvider(AuthProvider.LOCAL);
        admin.setActive(true);
        userRepository.save(admin);
        log.info("Administrator account created for {}", mask(email));
    }

    private static String mask(String email) {
        int at = email.indexOf('@');
        return at > 2 ? email.substring(0, 2) + "***" + email.substring(at) : "***";
    }
}
