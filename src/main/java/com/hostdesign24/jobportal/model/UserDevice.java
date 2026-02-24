package com.hostdesign24.jobportal.model;

import com.hostdesign24.jobportal.model.enums.DeviceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_devices", indexes = {
        @Index(name = "idx_user_device", columnList = "userId,deviceId"),
        @Index(name = "idx_user_status", columnList = "userId,deviceStatus")
})
@Getter
@Setter
public class UserDevice extends BaseEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private String deviceName;

    private String ipAddress;

    private String userAgent;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private LocalDateTime firstLogin;

    private LocalDateTime lastLogin;

    private LocalDateTime verifiedAt;

    private String blockReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus deviceStatus = DeviceStatus.ACTIVE;

    private String location;
}