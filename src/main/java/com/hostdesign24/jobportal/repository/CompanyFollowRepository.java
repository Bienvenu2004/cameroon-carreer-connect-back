package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.CompanyFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyFollowRepository extends JpaRepository<CompanyFollow, UUID> {

    Optional<CompanyFollow> findByCompanyIdAndProfileIdAndDeletedFalse(UUID companyId, UUID profileId);

    /**
     * A soft-deleted follow, so re-following restores the original row rather
     * than colliding with the unique constraint on (company_id, profile_id).
     */
    Optional<CompanyFollow> findByCompanyIdAndProfileId(UUID companyId, UUID profileId);

    /** The seeker's followed employers. The graph avoids an N+1 over the list. */
    @EntityGraph(attributePaths = {"company", "company.logo"})
    Page<CompanyFollow> findByProfileIdAndDeletedFalseOrderByFollowedAtDesc(UUID profileId, Pageable pageable);

    long countByCompanyIdAndDeletedFalse(UUID companyId);

    /**
     * Who to tell when this employer posts. Only follows that opted into email
     * are returned; in-app notification uses the unfiltered variant.
     */
    @Query("SELECT f FROM CompanyFollow f "
            + "JOIN FETCH f.profile p JOIN FETCH p.user "
            + "WHERE f.company.id = :companyId AND f.deleted = false")
    List<CompanyFollow> findFollowersToNotify(@Param("companyId") UUID companyId);

    /**
     * Which of these companies the seeker already follows, for rendering many
     * follow buttons without one request each.
     */
    @Query("SELECT f.company.id FROM CompanyFollow f "
            + "WHERE f.profile.id = :profileId AND f.deleted = false")
    List<UUID> findFollowedCompanyIds(@Param("profileId") UUID profileId);
}
