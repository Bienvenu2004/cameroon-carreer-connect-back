package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.JobInvitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobInvitationRepository extends JpaRepository<JobInvitation, UUID> {

    boolean existsByJobIdAndProfileIdAndDeletedFalse(UUID jobId, UUID profileId);

    /** A candidate's invitations, newest first. */
    @EntityGraph(attributePaths = {"job", "job.company"})
    Page<JobInvitation> findByProfileIdAndDeletedFalseOrderBySentAtDesc(UUID profileId, Pageable pageable);

    long countByProfileIdAndRespondedAtIsNullAndDeletedFalse(UUID profileId);
}
