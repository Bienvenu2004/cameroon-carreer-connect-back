package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.UserDto;
import com.hostdesign24.jobportal.dto.UserRegistrationDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.common.CreationResponse;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.services.UsersService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/hjp/users")
@RequiredArgsConstructor
public class UsersController {

    private final UsersService usersService;

    @PostMapping("/register")
    public ResponseEntity<CreationResponse> register(@Valid @RequestBody UserRegistrationDto dto) {

        Optional<User> existingUser = usersService.getUserByEmail(dto.getEmail());
        if (existingUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new CreationResponse(null, "Cannot register user with this email. Email already in use."));
        }

        CreationResponse response = new CreationResponse(usersService.addNew(dto).getId(), "User registered successfully");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser() {

        UserDto user = usersService.getCurrentUserDto();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(ApiResponse.success(user, "user fetched successfully"));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<Object>> getCurrentProfile() {

        Object profile = usersService.getCurrentUserProfile();

        if (profile == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(ApiResponse.success(profile, "user profile fetched successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }
}
