package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.enums.Region;
import com.hostdesign24.jobportal.model.enums.Industry;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.hostdesign24.jobportal.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {

    /* =====================================================================
     * Fetch graph for job listings.
     *
     * Job.company is a @ManyToOne (EAGER by default) and Company.logo is another
     * @ManyToOne, so rendering a page of 20 job cards previously issued one extra
     * SELECT per row for the company and another for its logo -- roughly 41
     * queries to display one page.
     *
     * Redeclaring the Specification query here lets us attach an entity graph to
     * it. Both associations are single-valued, so the join does not multiply rows
     * and pagination stays in the database.
     * ===================================================================== */
    @Override
    @EntityGraph(attributePaths = {"company", "company.logo"})
    Page<Job> findAll(Specification<Job> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"company", "company.logo"})
    @Query("SELECT j FROM Job j WHERE " +
           "(:title = '' OR LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:location = '' OR LOWER(j.location.city) LIKE LOWER(CONCAT('%', :location, '%')) " +
           "   OR LOWER(j.location.country) LIKE LOWER(CONCAT('%', :location, '%')) " +
           "   OR LOWER(j.location.stateRegion) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:date IS NULL OR j.postedDate >= :date)")
    List<Job> search(@Param("title") String title,
                     @Param("location") String location,
                     @Param("date") LocalDate date);

    List<Job> findByCompanyId(UUID companyId);

    int countByCompanyIdAndIsActiveTrueAndDeletedFalse(UUID companyId);

    @Query("SELECT COALESCE(SUM(j.views), 0) FROM Job j")
    Long getTotalViews();

    /**
     * Candidate pool for the AI recommendation engine.
     *
     *   - The job must be active and not soft-deleted
     *   - The company must be approved (no PENDING / REJECTED / SUSPENDED)
     *   - The given seeker profile must NOT have an existing application
     *     for the job (NOT EXISTS subquery on JobApplication.profile.id)
     *
     * Newest first so a fresh listing surfaces while it's still hot. The
     * caller passes a {@code Pageable} so we cap the result set well
     * before it would inflate the LLM prompt.
     */
    @Query("""
        SELECT j FROM Job j
         WHERE j.isActive = true
           AND j.deleted = false
           AND j.company.status = com.hostdesign24.jobportal.model.enums.CompanyStatus.APPROVED
           AND j.company.deleted = false
           AND NOT EXISTS (
                 SELECT a FROM JobApplication a
                  WHERE a.job.id = j.id
                    AND a.profile.id = :profileId
                    AND a.deleted = false
               )
         ORDER BY j.createdAt DESC
    """)
    List<Job> findAiCandidatePool(@Param("profileId") UUID profileId, Pageable pageable);

    /* =====================================================================
     * Dashboard aggregates -- see the note on UserRepository.
     * ===================================================================== */

    /**
     * Open listings whose deadline has passed, for the nightly expiry sweep.
     * Excludes jobs already closed so the pass is idempotent.
     */
    @Query("SELECT j FROM Job j "
            + "WHERE j.deleted = false AND j.isActive = true "
            + "AND j.applicationDeadline IS NOT NULL AND j.applicationDeadline < :today")
    List<Job> findLapsed(@Param("today") LocalDate today);

    /**
     * Other open listings from the same company, for the "more from this
     * employer" block on a job page. Excludes the job being viewed.
     */
    @Query("SELECT j FROM Job j "
            + "WHERE j.deleted = false AND j.isActive = true "
            + "AND j.company.id = :companyId AND j.id <> :excludeJobId "
            + "ORDER BY j.postedDate DESC")
    List<Job> findOtherOpenJobsAtCompany(@Param("companyId") UUID companyId,
                                         @Param("excludeJobId") UUID excludeJobId,
                                         Pageable pageable);

    /**
     * Listings similar to the one being viewed: same industry, preferring the
     * same region. Cheapest useful version of "you might also like" -- no
     * embeddings, no extra infrastructure, and it gives a visitor somewhere to
     * go other than the back button.
     */
    @Query("SELECT j FROM Job j "
            + "WHERE j.deleted = false AND j.isActive = true AND j.id <> :excludeJobId "
            + "AND (j.company.industry = :industry OR j.location.region = :region) "
            + "ORDER BY CASE WHEN j.location.region = :region THEN 0 ELSE 1 END, j.postedDate DESC")
    List<Job> findSimilar(@Param("excludeJobId") UUID excludeJobId,
                          @Param("industry") Industry industry,
                          @Param("region") Region region,
                          Pageable pageable);

    /** Open jobs at one company, for the "following" list. */
    @Query("SELECT COUNT(j) FROM Job j "
            + "WHERE j.deleted = false AND j.isActive = true AND j.company.id = :companyId")
    long countOpenAtCompany(@Param("companyId") UUID companyId);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.deleted = false")
    long countNotDeleted();

    @Query("SELECT COUNT(j) FROM Job j WHERE j.deleted = false AND j.isActive = true")
    long countActiveJobs();

    @Query("SELECT COUNT(j) FROM Job j WHERE j.deleted = false AND j.isActive = true AND j.createdBy = :creator")
    long countActiveJobsByCreator(@Param("creator") UUID creator);

    /** Jobs posted per calendar day since {@code from}; folded into months by the service. */
    @Query("SELECT j.postedDate, COUNT(j) FROM Job j "
            + "WHERE j.deleted = false AND j.postedDate >= :from "
            + "GROUP BY j.postedDate")
    List<Object[]> countPostedByDaySince(@Param("from") LocalDate from);

    /** Job counts per Cameroon region, as Object[]{ Region, Long }. */
    @Query("SELECT j.location.region, COUNT(j) FROM Job j "
            + "WHERE j.deleted = false AND j.location.region IS NOT NULL "
            + "GROUP BY j.location.region")
    List<Object[]> countByRegion();
}
