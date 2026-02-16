package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.JobCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobCompanyRepository extends JpaRepository<JobCompany, UUID> {
}
