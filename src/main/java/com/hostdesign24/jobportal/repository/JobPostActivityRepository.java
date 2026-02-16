package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface JobPostActivityRepository extends JpaRepository<JobPost, UUID>, JpaSpecificationExecutor<JobPost> {


    @Query(value = "SELECT * FROM job_post_activity j INNER JOIN job_location l on j.job_location_id=l.id  WHERE j" +
            ".job_title LIKE %:job%"
            + " AND (l.city LIKE %:location%"
            + " OR l.country LIKE %:location%"
            + " OR l.state LIKE %:location%) " +
            " AND (j.job_type IN(:type)) " +
            " AND (j.remote IN(:remote)) " +
            " AND (posted_date >= :date)", nativeQuery = true)
    List<JobPost> search(@Param("job") String job,
                         @Param("location") String location,
                         @Param("remote") List<String> remote,
                         @Param("type") List<String> type,
                         @Param("date") LocalDate searchDate);
}
