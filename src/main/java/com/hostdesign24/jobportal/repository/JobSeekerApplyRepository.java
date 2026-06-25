package com.hostdesign24.jobportal.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.hostdesign24.jobportal.model.JobApplication;

@Repository
public interface JobSeekerApplyRepository extends JpaRepository<JobApplication, UUID>,
        JpaSpecificationExecutor<JobApplication> {
    long countByJobId(UUID jobId);
}
