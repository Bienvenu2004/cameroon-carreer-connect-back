package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.JobSeekerProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public interface JobSeekerProfileRepository
        extends JpaRepository<JobSeekerProfile, UUID>, JpaSpecificationExecutor<JobSeekerProfile> {

    JobSeekerProfile findByUserId(UUID id);

    /**
     * Candidate search reads skills, education, experience and the photo for
     * every row it renders. Without the graph a page of twenty results issues
     * roughly eighty extra queries.
     */
    @Override
    @EntityGraph(attributePaths = {"skills", "educations", "experiences", "profilePhoto"})
    Page<JobSeekerProfile> findAll(Specification<JobSeekerProfile> spec, Pageable pageable);
}
