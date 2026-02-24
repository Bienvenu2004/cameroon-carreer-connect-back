package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.UserDto;
import com.hostdesign24.jobportal.dto.UserRegistrationDto;
import com.hostdesign24.jobportal.model.User;
import jakarta.validation.Valid;

import java.util.Optional;

public interface UsersService {
    User createUser(@Valid UserRegistrationDto dto);

    Object getCurrentUserProfile();

    User getCurrentUser();

    UserDto getCurrentUserDto();

    User findByEmail(String currentUsername);

    Optional<User> getUserByEmail(String email);

    boolean emailExists(String email);
}
