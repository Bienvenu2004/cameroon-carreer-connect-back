package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobSeekerApplyRepository extends JpaRepository<JobApplication, UUID>,
        JpaSpecificationExecutor<JobApplication> {
}
