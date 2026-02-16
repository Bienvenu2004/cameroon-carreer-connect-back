package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.JobSeekerSave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobSeekerSaveRepository extends JpaRepository<JobSeekerSave, UUID>,
        JpaSpecificationExecutor<JobSeekerSave> {
}
