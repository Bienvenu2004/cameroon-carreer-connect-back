package com.hostdesign24.jobportal.security;

import com.hostdesign24.jobportal.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class SpringSecurityAuditorAwareImpl implements AuditorAware<UUID> {
    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if(principal instanceof UUID) {
            return Optional.of((UUID) principal);
        }
        else if(principal instanceof User) {
            return Optional.of(((User) principal).getId());
        }

        if (principal instanceof String) {
            UUID userId = UUID.fromString((String) principal);
            return Optional.of(userId);
        }

        return Optional.empty();
    }
}
