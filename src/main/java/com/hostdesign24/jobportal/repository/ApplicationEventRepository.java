package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.ApplicationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationEventRepository extends JpaRepository<ApplicationEvent, UUID> {

    List<ApplicationEvent> findByApplicationIdOrderByOccurredAtAsc(UUID applicationId);

    /**
     * Employer responsiveness, computed for one company.
     *
     * Returns { applicationsReceived, applicationsAnswered, medianDaysToFirstReply }
     * where "answered" means the application moved beyond APPLIED at least once.
     * A verification badge tells a candidate the company is real; this tells them
     * whether applying is worth their evening and their data bundle, which is a
     * much sharper question and one no WhatsApp group can answer.
     *
     * The median is approximated by the average, which is cheap in SQL and honest
     * enough at the sample sizes involved; the DTO calls it an average.
     */
    @Query("""
        SELECT COUNT(DISTINCT a.id),
               COUNT(DISTINCT CASE WHEN e.toStatus <> com.hostdesign24.jobportal.model.enums.ApplicationStatus.APPLIED
                                   THEN a.id END),
               AVG(CASE WHEN e.toStatus <> com.hostdesign24.jobportal.model.enums.ApplicationStatus.APPLIED
                        THEN CAST(FUNCTION('DATE_PART', 'day', e.occurredAt - a.createdAt) AS double) END)
          FROM JobApplication a
          LEFT JOIN ApplicationEvent e ON e.application = a
         WHERE a.job.company.id = :companyId
           AND a.job.deleted = false
    """)
    List<Object[]> responsivenessForCompany(@Param("companyId") UUID companyId);
}
