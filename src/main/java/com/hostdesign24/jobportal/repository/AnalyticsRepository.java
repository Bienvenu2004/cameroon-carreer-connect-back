package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Read-only aggregate queries backing the analytics dashboards.
 *
 * These exist because {@code AnalyticsServiceImpl} used to call {@code findAll()}
 * on the jobs and applications tables and reduce them in Java -- and, worse, ran
 * one {@code countByJobId} query per job while building the per-job breakdown.
 * A recruiter with 40 postings issued 40 extra queries on every dashboard load.
 *
 * Every method here returns grouped rows computed by the database. Result size is
 * bounded by the number of distinct groups (statuses, regions, cities, days),
 * never by the number of applications.
 *
 * The dashboard is scoped three ways and JPQL cannot express "skip this predicate
 * when the parameter is null" without relying on driver-specific null-parameter
 * typing, so each scope gets its own explicit method. They are one-liners and the
 * naming makes the intent obvious at the call site:
 *
 *   *All            -- platform-wide (admin)
 *   *ByJobCreator   -- applications on jobs posted by one recruiter
 *   *BySeeker       -- one job seeker's own applications
 *
 * Rows are {@code Object[]} tuples; the service folds them into the DTO maps.
 */
@Repository
public interface AnalyticsRepository extends JpaRepository<JobApplication, UUID> {

    /* ===================================================================
     * Per-job breakdown: { jobId, title, views, applicationCount }
     *
     * A LEFT JOIN keeps jobs with zero applications in the result, which the
     * previous per-job count loop also did.
     * =================================================================== */

    @Query("""
        SELECT j.id, j.title, COALESCE(j.views, 0), COUNT(a.id)
          FROM Job j
          LEFT JOIN JobApplication a ON a.job = j
         WHERE j.deleted = false
         GROUP BY j.id, j.title, j.views
    """)
    List<Object[]> jobStatsAll();

    @Query("""
        SELECT j.id, j.title, COALESCE(j.views, 0), COUNT(a.id)
          FROM Job j
          LEFT JOIN JobApplication a ON a.job = j
         WHERE j.deleted = false AND j.createdBy = :creator
         GROUP BY j.id, j.title, j.views
    """)
    List<Object[]> jobStatsByCreator(@Param("creator") UUID creator);

    /* ===================================================================
     * Applications by status: { ApplicationStatus, count }
     * =================================================================== */

    @Query("SELECT a.status, COUNT(a) FROM JobApplication a "
            + "WHERE a.job.deleted = false GROUP BY a.status")
    List<Object[]> appStatusAll();

    @Query("SELECT a.status, COUNT(a) FROM JobApplication a "
            + "WHERE a.job.deleted = false AND a.job.createdBy = :creator GROUP BY a.status")
    List<Object[]> appStatusByJobCreator(@Param("creator") UUID creator);

    @Query("SELECT a.status, COUNT(a) FROM JobApplication a "
            + "WHERE a.profile.user.id = :seeker GROUP BY a.status")
    List<Object[]> appStatusBySeeker(@Param("seeker") UUID seeker);

    /* ===================================================================
     * Applicant demographics: { city, country, count }
     * =================================================================== */

    @Query("SELECT a.profile.address.city, a.profile.address.country, COUNT(a) FROM JobApplication a "
            + "WHERE a.job.deleted = false "
            + "AND (a.profile.address.city IS NOT NULL OR a.profile.address.country IS NOT NULL) "
            + "GROUP BY a.profile.address.city, a.profile.address.country")
    List<Object[]> appDemographicsAll();

    @Query("SELECT a.profile.address.city, a.profile.address.country, COUNT(a) FROM JobApplication a "
            + "WHERE a.job.deleted = false AND a.job.createdBy = :creator "
            + "AND (a.profile.address.city IS NOT NULL OR a.profile.address.country IS NOT NULL) "
            + "GROUP BY a.profile.address.city, a.profile.address.country")
    List<Object[]> appDemographicsByJobCreator(@Param("creator") UUID creator);

    @Query("SELECT a.profile.address.city, a.profile.address.country, COUNT(a) FROM JobApplication a "
            + "WHERE a.profile.user.id = :seeker "
            + "AND (a.profile.address.city IS NOT NULL OR a.profile.address.country IS NOT NULL) "
            + "GROUP BY a.profile.address.city, a.profile.address.country")
    List<Object[]> appDemographicsBySeeker(@Param("seeker") UUID seeker);

    /* ===================================================================
     * Applications per day since a cutoff: { LocalDate, count }
     * Folded into months by the service.
     * =================================================================== */

    @Query("SELECT a.applicationDate, COUNT(a) FROM JobApplication a "
            + "WHERE a.job.deleted = false AND a.applicationDate >= :from "
            + "GROUP BY a.applicationDate")
    List<Object[]> appsByDayAll(@Param("from") LocalDate from);

    @Query("SELECT a.applicationDate, COUNT(a) FROM JobApplication a "
            + "WHERE a.job.deleted = false AND a.job.createdBy = :creator AND a.applicationDate >= :from "
            + "GROUP BY a.applicationDate")
    List<Object[]> appsByDayByJobCreator(@Param("creator") UUID creator, @Param("from") LocalDate from);

    @Query("SELECT a.applicationDate, COUNT(a) FROM JobApplication a "
            + "WHERE a.profile.user.id = :seeker AND a.applicationDate >= :from "
            + "GROUP BY a.applicationDate")
    List<Object[]> appsByDayBySeeker(@Param("seeker") UUID seeker, @Param("from") LocalDate from);

    /* ===================================================================
     * Regional dashboard.
     *
     * All five series are group-bys over small key spaces (10 regions ×
     * distinct company / skill names), so the whole regional page is five
     * queries instead of two full table scans plus in-memory joins.
     * =================================================================== */

    /** { Region, count } over active, non-deleted jobs. */
    @Query("SELECT j.location.region, COUNT(j) FROM Job j "
            + "WHERE j.deleted = false AND j.isActive = true AND j.location.region IS NOT NULL "
            + "GROUP BY j.location.region")
    List<Object[]> activeJobsByRegion();

    /** { Region, companyName, count } over active, non-deleted jobs. */
    @Query("SELECT j.location.region, j.company.name, COUNT(j) FROM Job j "
            + "WHERE j.deleted = false AND j.isActive = true "
            + "AND j.location.region IS NOT NULL AND j.company.name IS NOT NULL "
            + "GROUP BY j.location.region, j.company.name")
    List<Object[]> activeJobsByRegionAndCompany();

    /** { JobLanguage, count } over active, non-deleted jobs. */
    @Query("SELECT j.requiredLanguage, COUNT(j) FROM Job j "
            + "WHERE j.deleted = false AND j.isActive = true AND j.requiredLanguage IS NOT NULL "
            + "GROUP BY j.requiredLanguage")
    List<Object[]> activeJobsByLanguage();

    /** { Region, count } over applications to non-deleted jobs. */
    @Query("SELECT a.job.location.region, COUNT(a) FROM JobApplication a "
            + "WHERE a.job.deleted = false AND a.job.location.region IS NOT NULL "
            + "GROUP BY a.job.location.region")
    List<Object[]> applicationsByRegion();

    /**
     * { Region, skillName, count } -- how often each skill appears among the
     * applicants to jobs in a region. The join through the profile's skill
     * collection is done by the database rather than by walking a lazy list
     * per application.
     */
    @Query("SELECT j.location.region, s.name, COUNT(a) "
            + "FROM JobApplication a JOIN a.job j JOIN a.profile p JOIN p.skills s "
            + "WHERE j.deleted = false AND j.location.region IS NOT NULL "
            + "AND s.name IS NOT NULL AND s.name <> '' "
            + "GROUP BY j.location.region, s.name")
    List<Object[]> applicantSkillsByRegion();
}
