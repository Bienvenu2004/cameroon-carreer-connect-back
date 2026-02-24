package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.UserDevice;
import com.hostdesign24.jobportal.model.enums.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID>, JpaSpecificationExecutor<UserDevice> {

    Optional<UserDevice> findByUserIdAndDeviceId(UUID userId, String deviceId);

    Optional<UserDevice> findByUserIdAndDeviceIdAndDeviceStatus(
            UUID userId, String deviceId, DeviceStatus status);

    List<UserDevice> findByUserIdAndDeviceStatusNot(UUID userId, DeviceStatus status);

    List<UserDevice> findByUserId(UUID userId);

    Optional<UserDevice> findByUserIdAndDeviceIdAndVerifiedIsTrue(UUID userId, String deviceId);
}