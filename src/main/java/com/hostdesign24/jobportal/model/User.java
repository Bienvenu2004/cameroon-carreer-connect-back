package com.hostdesign24.jobportal.model;

import com.hostdesign24.jobportal.model.enums.AuthProvider;
import com.hostdesign24.jobportal.model.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {
    @Column(unique = true)
    private String email;

    @NotEmpty
    private String password;

    private boolean isActive = false;

    private LocalDate registrationDate = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @NotNull
    private UserRole role;

    /**
     * How this account authenticates. Defaults to LOCAL (email+password);
     * set to GOOGLE for accounts created through Google Sign-In. Google
     * accounts still carry a random encoded password so the NOT NULL /
     * @NotEmpty contract on {@code password} holds — it is simply never used.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    private Date passwordChangedAt;

    private Date lastLogin;

    private Date lastTokenRefresh;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private JobSeekerProfile jobSeekerProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private RecruiterProfile recruiterProfile;
}