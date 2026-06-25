package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.WorkExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link WorkExperience}.
 *
 * Most queries go through the parent {@code JobSeekerProfile} (cascaded
 * save / orphanRemoval keep the list in sync), so this is intentionally
 * slim. The one helper we expose is {@code findByProfileId} — needed by
 * {@code LLMContextBuilder} when it computes the "years of experience"
 * signal without having to hydrate the full profile graph.
 */
@Repository
public interface WorkExperienceRepository extends JpaRepository<WorkExperience, UUID> {

    /** All non-deleted experiences for a profile, newest start first. */
    List<WorkExperience> findByProfileIdAndDeletedFalseOrderByStartDateDesc(UUID profileId);
}
