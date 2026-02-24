package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.AuthenticationResponse;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.userDevice.LoginActivityDto;
import com.hostdesign24.jobportal.dto.userDevice.LoginActivityFilterDto;
import com.hostdesign24.jobportal.dto.userDevice.UserDeviceDto;
import com.hostdesign24.jobportal.dto.userDevice.UserDeviceFilterDto;
import com.hostdesign24.jobportal.exception.DeviceBlockedException;
import com.hostdesign24.jobportal.exception.DeviceIdGenerationException;
import com.hostdesign24.jobportal.mapper.LoginActivityMapper;
import com.hostdesign24.jobportal.mapper.UserDeviceMapper;
import com.hostdesign24.jobportal.model.LoginActivity;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.UserDevice;
import com.hostdesign24.jobportal.model.enums.DeviceStatus;
import com.hostdesign24.jobportal.repository.LoginActivityRepository;
import com.hostdesign24.jobportal.repository.UserDeviceRepository;
import com.hostdesign24.jobportal.repository.specifications.DeviceSpecification;
import com.hostdesign24.jobportal.repository.specifications.LoginActivitySpecification;
import com.hostdesign24.jobportal.services.EmailVerificationService;
import com.hostdesign24.jobportal.services.GeolocationService;
import com.hostdesign24.jobportal.services.UserDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDeviceServiceImpl implements UserDeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final EmailVerificationService emailVerificationService;

    private static final LinkedHashMap<String, String> OS_MATCHERS = new LinkedHashMap<>();
    private static final LinkedHashMap<String, String> ANDROID_MANUFACTURERS = new LinkedHashMap<>();

    static {
        OS_MATCHERS.put("windows nt 10.0", "Windows 10/11");
        OS_MATCHERS.put("windows nt 6.3", "Windows 8.1");
        OS_MATCHERS.put("windows nt 6.2", "Windows 8");
        OS_MATCHERS.put("windows nt 6.1", "Windows 7");
        OS_MATCHERS.put("windows", "Windows");
        OS_MATCHERS.put("mac os x 10_15", "macOS Catalina");
        OS_MATCHERS.put("mac os x 11", "macOS Big Sur");
        OS_MATCHERS.put("mac os x 12", "macOS Monterey");
        OS_MATCHERS.put("mac os x 13", "macOS Ventura");
        OS_MATCHERS.put("mac os x 14", "macOS Sonoma");
        OS_MATCHERS.put("mac os x 15", "macOS Sequoia");
        OS_MATCHERS.put("mac", "macOS");
        OS_MATCHERS.put("ubuntu", "Ubuntu");
        OS_MATCHERS.put("linux", "Linux");
        OS_MATCHERS.put("android", "Android");
        OS_MATCHERS.put("ios", "iOS");
        OS_MATCHERS.put("iphone", "iOS");
        OS_MATCHERS.put("ipad", "iOS");

        ANDROID_MANUFACTURERS.put("samsung", "Samsung");
        ANDROID_MANUFACTURERS.put("pixel", "Google Pixel");
        ANDROID_MANUFACTURERS.put("huawei", "Huawei");
        ANDROID_MANUFACTURERS.put("xiaomi", "Xiaomi");
        ANDROID_MANUFACTURERS.put("oneplus", "OnePlus");
        ANDROID_MANUFACTURERS.put("oppo", "Oppo");
        ANDROID_MANUFACTURERS.put("vivo", "Vivo");
    }

    private final LoginActivityRepository loginActivityRepository;
    private final DeviceSpecification deviceSpecification;
    private final UserDeviceMapper userDeviceMapper;
    private final LoginActivitySpecification loginActivitySpecification;
    private final LoginActivityMapper loginActivityMapper;
    private final GeolocationService geolocationService;

    @Override
    public String generateDeviceId(String userAgent, String ipAddress) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(userAgent.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating device ID", e);
            throw new DeviceIdGenerationException("Failed to generate device ID", e);
        }
    }

    @Override
    public String extractDeviceName(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown Device";
        }

        String ua = userAgent.toLowerCase();
        String device = extractDevice(ua);
        String browser = extractBrowser(ua);
        String os = extractOS(ua);

        if (!"Desktop".equals(device)) {
            return String.format("%s • %s", browser, device);
        }
        return String.format("%s • %s", browser, os);
    }

    @Override
    public boolean isDeviceVerified(UUID userId, String deviceId) {
        return userDeviceRepository
                .findByUserIdAndDeviceIdAndDeviceStatus(userId, deviceId, DeviceStatus.ACTIVE)
                .map(UserDevice::isVerified)
                .orElse(false);
    }

    @Override
    public boolean isDeviceBlocked(UUID userId, String deviceId) {
        return userDeviceRepository
                .findByUserIdAndDeviceId(userId, deviceId)
                .map(device -> device.getDeviceStatus() == DeviceStatus.BLOCKED)
                .orElse(false);
    }

    @Override
    public Optional<UserDevice> getDevice(UUID userId, String deviceId) {
        return userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId);
    }

    @Override
    public UserDevice registerNewDevice(UUID userId, String deviceId, String deviceName,
                                        String ipAddress, String userAgent) {
        Optional<UserDevice> existingDevice = userDeviceRepository
                .findByUserIdAndDeviceId(userId, deviceId);

        if (existingDevice.isPresent()) {
            UserDevice existingDeviceGet = existingDevice.get();
            if (existingDeviceGet.getDeviceStatus() == DeviceStatus.BLOCKED) {
                throw new DeviceBlockedException("Cannot register a blocked device");
            }
            existingDeviceGet.setLastLogin(LocalDateTime.now());
            existingDeviceGet.setIpAddress(ipAddress);
            existingDeviceGet.setDeviceStatus(DeviceStatus.ACTIVE);
            return userDeviceRepository.save(existingDeviceGet);
        }

        UserDevice device = new UserDevice();
        device.setUserId(userId);
        device.setDeviceId(deviceId);
        device.setDeviceName(deviceName);
        device.setIpAddress(ipAddress);
        device.setUserAgent(userAgent);
        device.setVerified(false);
        device.setFirstLogin(LocalDateTime.now());
        device.setLastLogin(LocalDateTime.now());
        device.setDeviceStatus(DeviceStatus.ACTIVE);
        device.setLocation(geolocationService.getLocationFromIP(ipAddress));

        UserDevice savedDevice = userDeviceRepository.save(device);
        log.info("New device registered for user ID: {} - Device: {}", userId, deviceName);

        return savedDevice;
    }

    @Transactional
    @Override
    public void verifyDevice(UUID userId, String deviceId) {
        UserDevice device = userDeviceRepository
                .findByUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        if (device.getDeviceStatus() == DeviceStatus.BLOCKED) {
            throw new IllegalStateException("Cannot verify a blocked device");
        }

        device.setVerified(true);
        device.setVerifiedAt(LocalDateTime.now());
        device.setDeviceStatus(DeviceStatus.ACTIVE);
        userDeviceRepository.save(device);

        log.info("Device verified for user ID: {} - Device: {}", userId, device.getDeviceName());
    }

    @Override
    public PageResponseDto<UserDeviceDto> getUserDevicesDto(UserDeviceFilterDto filter) {
        Page<UserDevice> devicePage = getAllDevices(filter);

        if (devicePage.isEmpty()) {
            return new PageResponseDto<>(
                    Collections.emptyList(),
                    devicePage.getNumber(),
                    devicePage.getSize(),
                    0L,
                    0,
                    true
            );
        }

        List<UserDeviceDto> devices = devicePage.getContent().stream()
                .map(d -> {
                    UserDeviceDto dto = userDeviceMapper.toDto(d);
                    if (d.getLocation() == null ){
                        dto.setLocation(geolocationService.getLocationFromIP(d.getIpAddress()));
                    }
                    return dto;
                })
                .toList();

        return new PageResponseDto<>(
                devices,
                devicePage.getNumber(),
                devicePage.getSize(),
                devicePage.getTotalElements(),
                devicePage.getTotalPages(),
                devicePage.isLast()
        );
    }

    private Page<UserDevice> getAllDevices(UserDeviceFilterDto filter) {
        Specification<UserDevice> spec = deviceSpecification.build(filter);

        return userDeviceRepository.findAll(spec, filter.toPageable());
    }

    @Override
    public void updateDeviceStatus(UUID userId, UserDevice device, DeviceStatus status) {
        device.setDeviceStatus(status);
        userDeviceRepository.save(device);
        log.info("Device status updated for user ID: {} - Device: {} - Status: {}",
                userId, device.getDeviceName(), status);
    }

    private static User getCurrentUser() {
        return Utils.getCurrentUser().orElseThrow(
                () -> new SecurityException("No authenticated user found")
        );
    }

    @Transactional
    @Override
    public void blockDevice(UUID id, String reason) {
        User currentUser = getCurrentUser();
        userDeviceRepository.findById(id).ifPresent(
                device -> {
                    device.setBlockReason(reason);
                    updateDeviceStatus(currentUser.getId(), device, DeviceStatus.BLOCKED);
                }
        );
    }

    @Transactional
    @Override
    public void unblockDevice(UUID id) {
        User currentUser = getCurrentUser();
        userDeviceRepository.findById(id).ifPresent(
                device -> updateDeviceStatus(currentUser.getId(), device, DeviceStatus.ACTIVE)
        );

    }

    private String extractDevice(String ua) {
        if (ua.contains("iphone")) {
            // Check for major iOS versions in order
            if (ua.contains("iphone os 18")) return "iPhone (iOS 18)";
            if (ua.contains("iphone os 17")) return "iPhone (iOS 17)";
            if (ua.contains("iphone os 16")) return "iPhone (iOS 16)";
            return "iPhone";
        }

        if (ua.contains("ipad")) {
            return "iPad";
        }

        if (ua.contains("android")) {
            String manuf = matchFirst(ua, ANDROID_MANUFACTURERS);
            return manuf != null ? manuf : "Android";
        }

        return "Desktop";
    }

    private String extractBrowser(String userAgent) {
        if (userAgent.contains("edg/") || userAgent.contains("edge/")) return "Edge";
        if (userAgent.contains("chrome/") && !userAgent.contains("edg")) return "Chrome";
        if (userAgent.contains("firefox/")) return "Firefox";
        if (userAgent.contains("safari/") && !userAgent.contains("chrome")) return "Safari";
        if (userAgent.contains("opera") || userAgent.contains("opr/")) return "Opera";
        if (userAgent.contains("brave")) return "Brave";
        return "Unknown Browser";
    }

    private String extractOS(String ua) {
        String match = matchFirst(ua, OS_MATCHERS);
        return match != null ? match : "Unknown OS";
    }

    private String matchFirst(String ua, Map<String, String> orderedMatchers) {
        for (Map.Entry<String, String> entry : orderedMatchers.entrySet()) {
            if (ua.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void updateDeviceLastLogin(UUID userId, UserDevice device, String ipAddress) {
        device.setLastLogin(LocalDateTime.now());
        device.setIpAddress(ipAddress);
        userDeviceRepository.save(device);
        log.debug("Updated login info for device {} - User: {}", device.getDeviceId(), userId);
    }

    @Override
    public AuthenticationResponse initiateDeviceVerification(User user, String deviceId, String deviceName, String ip, String userAgent) {
        registerNewDevice(user.getId(), deviceId, deviceName, ip, userAgent);

        emailVerificationService.initiateEmailDeviceVerification(
                user.getEmail(), deviceId, deviceName, ip);

        log.info("New device detected for user {} from IP {}. Verification required. Device: {}",
                user.getEmail(), ip, deviceName);

        return AuthenticationResponse.builder()
                .requiresDeviceVerification(true)
                .deviceId(deviceId)
                .message("Device verification required. A verification code has been sent to your email.")
                .build();
    }

    @Override
    public void recordLoginActivity(User user, String deviceId, String ip, boolean success, String failureReason) {
        UUID userId = user != null ? user.getId() : null;
        if (userId == null) {
            log.debug("recordLoginActivity called with null user");
            return;
        }

        userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId).ifPresent(device -> {
            updateDeviceLastLogin(userId, device, ip);

            LoginActivity activity = new LoginActivity();
            activity.setUserDevice(device);
            activity.setUserId(userId);
            activity.setIpAddress(ip);
            if (!success) {
                activity.markFailed(failureReason);
            }
            loginActivityRepository.save(activity);

            if (success) {
                log.info("Successful login for user {} device {} from ip {}", user.getEmail(), deviceId, ip);
            } else {
                log.warn("Failed login attempt for user {} device {} from ip {} - reason: {}", user.getEmail(), deviceId, ip, failureReason);
            }
        });
    }

    @Override
    public PageResponseDto<LoginActivityDto> getLoginActivitiesDto(LoginActivityFilterDto filter) {
        Page<LoginActivity> activityPage = getLoginActivities(filter);

        if (activityPage.isEmpty()) {
            return new PageResponseDto<>(
                    Collections.emptyList(),
                    activityPage.getNumber(),
                    activityPage.getSize(),
                    0L,
                    0,
                    true
            );
        }

        List<LoginActivityDto> devices = activityPage.getContent().stream()
                .map(activity -> {
                    LoginActivityDto dto = loginActivityMapper.toDto(activity);
                    UserDevice userDevice = activity.getUserDevice();
                    dto.setDevice(userDeviceMapper.toDto(userDevice));
                    return dto;
                })
                .toList();

        return new PageResponseDto<>(
                devices,
                activityPage.getNumber(),
                activityPage.getSize(),
                activityPage.getTotalElements(),
                activityPage.getTotalPages(),
                activityPage.isLast()
        );
    }

    private Page<LoginActivity> getLoginActivities(LoginActivityFilterDto filter) {
        Specification<LoginActivity> spec = loginActivitySpecification.build(filter);

        return loginActivityRepository.findAll(spec, filter.toPageable());
    }

    @Override
    public void signOutDevice(UUID id) {

    }
}