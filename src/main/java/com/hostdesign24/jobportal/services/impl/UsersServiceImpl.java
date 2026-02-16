package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.dto.UserDto;
import com.hostdesign24.jobportal.dto.UserRegistrationDto;
import com.hostdesign24.jobportal.mapper.UserMapper;
import com.hostdesign24.jobportal.model.JobSeekerProfile;
import com.hostdesign24.jobportal.model.RecruiterProfile;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.UserRole;
import com.hostdesign24.jobportal.repository.JobSeekerProfileRepository;
import com.hostdesign24.jobportal.repository.RecruiterProfileRepository;
import com.hostdesign24.jobportal.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsersServiceImpl implements com.hostdesign24.jobportal.services.UsersService {

    private final UserRepository userRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final PasswordEncoder passwordEncoder;

    private final UserMapper userMapper;

    @Override
    public User addNew(@Valid UserRegistrationDto dto) {
        User user = userMapper.toRegistrationUser(dto);
        user.setActive(true);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        User savedUser = userRepository.save(user);

        if (user.getRole().equals(UserRole.RECRUITER)) {
            recruiterProfileRepository.save(new RecruiterProfile(savedUser));
        } else {
            jobSeekerProfileRepository.save(new JobSeekerProfile(savedUser));
        }

        return savedUser;
    }

    @Override
    public Object getCurrentUserProfile() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            assert authentication != null;
            String username = authentication.getName();
            User user = userRepository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("Could not found user"));
            UUID userId = user.getId();

            if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("Recruiter"))) {
                return recruiterProfileRepository.findById(userId).orElse(new RecruiterProfile(user));
            } else {
                return jobSeekerProfileRepository.findById(userId).orElse(new JobSeekerProfile(user));
            }
        }

        return null;
    }

    @Override
    public User getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            assert authentication != null;
            String username = authentication.getName();
            return userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Could not found user"));
        }

        return null;
    }

    @Override
    public UserDto getCurrentUserDto() {
        User currentUser = getCurrentUser();
        return userMapper.toUserDto(currentUser);
    }

    @Override
    public User findByEmail(String currentUsername) {
        return userRepository.findByEmail(currentUsername).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

}