package com.hostdesign24.jobportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

    @Getter
    @Setter
    @Entity
    @Table(name = "login_activities")
    public class LoginActivity extends BaseEntity {
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_device_id", nullable = false)
        private UserDevice userDevice;

        @Column(name = "user_id", nullable = false)
        private UUID userId;

        @Column(name = "ip_address", length = 45)
        private String ipAddress;

        @Column(name = "successful", nullable = false)
        private boolean successful = true;

        @Column(name = "failure_reason", length = 512)
        private String failureReason;

        public void markFailed(String reason) {
            this.successful = false;
            this.failureReason = reason;
        }
    }