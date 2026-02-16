package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.RecruiterProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, UUID> {
    RecruiterProfile findByUserId(UUID id);
}
