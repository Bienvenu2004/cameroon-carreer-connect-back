package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.DeleteAccountRequest;
import com.hostdesign24.jobportal.dto.UserDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.services.AccountDeletionService;
import com.hostdesign24.jobportal.services.AuthService;
import com.hostdesign24.jobportal.services.UsersService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hjp/users")
@RequiredArgsConstructor
public class UsersController {

    private final UsersService usersService;
    private final AccountDeletionService accountDeletionService;
    private final AuthService authService;

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

    /**
     * Deletes the caller's own account, then ends the session the same way a
     * logout does: the token is blacklisted and the auth cookies are cleared.
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteCurrentAccount(@RequestBody DeleteAccountRequest body,
                                                                  HttpServletRequest request,
                                                                  HttpServletResponse response) {
        accountDeletionService.deleteCurrentAccount(body);
        authService.logout(request, response);
        return ResponseEntity.ok(ApiResponse.success("Account deleted"));
    }
}
