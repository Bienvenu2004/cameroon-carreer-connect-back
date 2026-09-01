package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EducationRepository extends JpaRepository<Education, UUID> {

    List<Education> findByProfileIdAndDeletedFalse(UUID profileId);
}
