package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.Job;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Reads for the employer activity feed.
 *
 * Every query here derives events from data the platform already records —
 * there is no events table and no write path. That is a deliberate choice, not
 * a shortcut:
 *
 *   - It cannot drift. A materialised feed that misses a write shows a false
 *     history; this one is the history, by construction.
 *   - It needed no migration and no backfill. The feed had roughly forty events
 *     the moment it existed, which is the difference between a feature that
 *     looks alive on a young platform and one that looks abandoned.
 *   - Nothing is authored, so nothing needs moderating.
 *
 * Each query is bounded by a Pageable and the service merges the results, so
 * the cost is a handful of indexed reads rather than a scan.
 *
 * The "InCompanies" variants exist because JPQL cannot express "skip this
 * predicate when the collection is empty", and an empty IN list is a syntax
 * error rather than a match-nothing. The service picks the variant.
 *
 * Rows are Object[] tuples shaped
 * { jobId, jobTitle, companyId, companyName, region, occurredAt }, with jobId
 * and jobTitle null for company-level events.
 */
@Repository
public interface ActivityFeedRepository extends JpaRepository<Job, UUID> {

    /* ------------------------------------------------------------------
     * Jobs posted
     * ------------------------------------------------------------------ */

    @Query("""
        SELECT j.id, j.title, c.id, c.name, j.location.region, j.createdAt
          FROM Job j JOIN j.company c
         WHERE j.deleted = false AND j.isActive = true AND c.deleted = false
         ORDER BY j.createdAt DESC
    """)
    List<Object[]> recentJobPosts(Pageable pageable);

    @Query("""
        SELECT j.id, j.title, c.id, c.name, j.location.region, j.createdAt
          FROM Job j JOIN j.company c
         WHERE j.deleted = false AND j.isActive = true AND c.deleted = false
           AND c.id IN :companyIds
         ORDER BY j.createdAt DESC
    """)
    List<Object[]> recentJobPostsInCompanies(@Param("companyIds") Collection<UUID> companyIds,
                                             Pageable pageable);

    /* ------------------------------------------------------------------
     * Employers verified
     * ------------------------------------------------------------------ */

    @Query("""
        SELECT NULL, NULL, c.id, c.name, c.address.region, c.verifiedAt
          FROM Company c
         WHERE c.deleted = false AND c.verifiedAt IS NOT NULL
           AND c.status = com.hostdesign24.jobportal.model.enums.CompanyStatus.APPROVED
         ORDER BY c.verifiedAt DESC
    """)
    List<Object[]> recentVerifications(Pageable pageable);

    @Query("""
        SELECT NULL, NULL, c.id, c.name, c.address.region, c.verifiedAt
          FROM Company c
         WHERE c.deleted = false AND c.verifiedAt IS NOT NULL
           AND c.status = com.hostdesign24.jobportal.model.enums.CompanyStatus.APPROVED
           AND c.id IN :companyIds
         ORDER BY c.verifiedAt DESC
    """)
    List<Object[]> recentVerificationsInCompanies(@Param("companyIds") Collection<UUID> companyIds,
                                                  Pageable pageable);

    /* ------------------------------------------------------------------
     * Roles filled through the platform
     *
     * Read from jobs.closedByHire rather than from the HIRED application event,
     * because the flag predates the event table and so covers hires made before
     * that history existed.
     * ------------------------------------------------------------------ */

    @Query("""
        SELECT j.id, j.title, c.id, c.name, j.location.region, j.updatedAt
          FROM Job j JOIN j.company c
         WHERE j.deleted = false AND j.closedByHire = true AND c.deleted = false
         ORDER BY j.updatedAt DESC
    """)
    List<Object[]> recentHires(Pageable pageable);

    @Query("""
        SELECT j.id, j.title, c.id, c.name, j.location.region, j.updatedAt
          FROM Job j JOIN j.company c
         WHERE j.deleted = false AND j.closedByHire = true AND c.deleted = false
           AND c.id IN :companyIds
         ORDER BY j.updatedAt DESC
    """)
    List<Object[]> recentHiresInCompanies(@Param("companyIds") Collection<UUID> companyIds,
                                          Pageable pageable);
}
