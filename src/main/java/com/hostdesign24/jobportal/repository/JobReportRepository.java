package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.JobReport;
import com.hostdesign24.jobportal.model.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobReportRepository extends JpaRepository<JobReport, UUID> {

    /** Moderation queue. The graph avoids an N+1 over job and company per row. */
    @EntityGraph(attributePaths = {"job", "job.company"})
    Page<JobReport> findByStatusAndDeletedFalseOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"job", "job.company"})
    Page<JobReport> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    long countByStatusAndDeletedFalse(ReportStatus status);

    /** One person flagging the same listing twice adds nothing to the queue. */
    boolean existsByJobIdAndReporterIdAndDeletedFalse(UUID jobId, UUID reporterId);

    long countByJobIdAndDeletedFalse(UUID jobId);
}
