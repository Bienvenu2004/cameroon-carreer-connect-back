package com.hostdesign24.jobportal.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.hostdesign24.jobportal.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {

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

    @Query("SELECT COALESCE(SUM(j.views), 0) FROM Job j")
    Long getTotalViews();
}
