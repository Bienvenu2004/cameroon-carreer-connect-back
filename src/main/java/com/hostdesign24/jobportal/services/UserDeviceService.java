package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.AuthenticationResponse;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.userDevice.LoginActivityDto;
import com.hostdesign24.jobportal.dto.userDevice.LoginActivityFilterDto;
import com.hostdesign24.jobportal.dto.userDevice.UserDeviceDto;
import com.hostdesign24.jobportal.dto.userDevice.UserDeviceFilterDto;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.UserDevice;
import com.hostdesign24.jobportal.model.enums.DeviceStatus;

import java.util.Optional;
import java.util.UUID;

public interface UserDeviceService {

    String generateDeviceId(String userAgent, String ipAddress);

    String extractDeviceName(String userAgent);

    boolean isDeviceVerified(UUID userId, String deviceId);

    boolean isDeviceBlocked(UUID userId, String deviceId);

    Optional<UserDevice> getDevice(UUID userId, String deviceId);

    UserDevice registerNewDevice(UUID userId, String deviceId, String deviceName,
                                 String ipAddress, String userAgent);

    void verifyDevice(UUID userId, String deviceId);

    PageResponseDto<UserDeviceDto> getUserDevicesDto(UserDeviceFilterDto filter);

    void updateDeviceStatus(UUID userId, UserDevice device, DeviceStatus status);

    void blockDevice(UUID id, String reason);

    void unblockDevice(UUID id);

    AuthenticationResponse initiateDeviceVerification(User user, String deviceId, String deviceName, String ip, String userAgent);

    void recordLoginActivity(User user, String deviceId, String ip, boolean success, String failureReason);

    PageResponseDto<LoginActivityDto> getLoginActivitiesDto(LoginActivityFilterDto filter);

    void signOutDevice(UUID id);
}