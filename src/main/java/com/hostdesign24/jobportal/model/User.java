    package com.hostdesign24.jobportal.model;

    import com.hostdesign24.jobportal.model.enums.UserRole;
    import jakarta.persistence.*;
    import jakarta.validation.constraints.NotEmpty;
    import jakarta.validation.constraints.NotNull;
    import lombok.*;

    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.util.Date;

    @Entity
    @Table(name = "users")
    @Getter
    @Setter
    @ToString(exclude = {"password"})
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

        @Temporal(TemporalType.TIMESTAMP)
        private Date passwordChangedAt;
    }