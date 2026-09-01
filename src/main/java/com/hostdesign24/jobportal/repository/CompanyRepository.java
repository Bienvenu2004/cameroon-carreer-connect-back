package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID>, JpaSpecificationExecutor<Company> {

    /** Company counts per verification status, as Object[]{ CompanyStatus, Long }. */
    @Query("SELECT c.status, COUNT(c) FROM Company c WHERE c.deleted = false GROUP BY c.status")
    List<Object[]> countByStatus();
}
