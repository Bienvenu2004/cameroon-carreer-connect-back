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
     *
     * Native SQL rather than JPQL: HQL compiles a timestamp subtraction into a
     * numeric duration, and Postgres has no date_part(text, numeric). Interval
     * arithmetic is clearer written out, and this is a reporting query against
     * one known database rather than portable domain logic.
     */
    @Query(value = """
        SELECT COUNT(DISTINCT a.id),
               COUNT(DISTINCT CASE WHEN e.to_status <> 'APPLIED' THEN a.id END),
               AVG(CASE WHEN e.to_status <> 'APPLIED'
                        THEN EXTRACT(EPOCH FROM (e.occurred_at - a.created_at)) / 86400.0 END)
          FROM job_applications a
          LEFT JOIN application_events e ON e.application_id = a.id
          JOIN jobs j ON j.id = a.job_id
         WHERE j.company_id = :companyId
           AND j.deleted = false
    """, nativeQuery = true)
    List<Object[]> responsivenessForCompany(@Param("companyId") UUID companyId);
}
