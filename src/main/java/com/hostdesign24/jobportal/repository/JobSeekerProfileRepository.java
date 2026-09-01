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
     * every row it renders.
     *
     * Only the photo is fetched here. The three collections are batched instead
     * (see @BatchSize on JobSeekerProfile): Hibernate cannot fetch more than one
     * bag in a single query, and joining three of them would multiply rows even
     * if it could. Batching turns the N+1 into one extra query per collection
     * per page, which is the outcome the graph was reaching for anyway.
     */
    @Override
    @EntityGraph(attributePaths = {"profilePhoto"})
    Page<JobSeekerProfile> findAll(Specification<JobSeekerProfile> spec, Pageable pageable);
}
