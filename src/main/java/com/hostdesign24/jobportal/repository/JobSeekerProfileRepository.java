package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.JobSeekerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobSeekerProfileRepository extends JpaRepository<JobSeekerProfile, UUID> {
    JobSeekerProfile findByUserId(UUID id);
}
