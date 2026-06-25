package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.UserDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.services.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hjp/users")
@RequiredArgsConstructor
public class UsersController {

    private final UsersService usersService;

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
}
