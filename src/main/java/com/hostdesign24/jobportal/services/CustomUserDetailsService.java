package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.repository.UserRepository;
import com.hostdesign24.jobportal.util.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        String normalizedEmail = username.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailAndDeletedFalse(normalizedEmail)
                .orElseThrow(
                        () -> new UsernameNotFoundException("Could not found user"));
        return new CustomUserDetails(user);
    }
}
