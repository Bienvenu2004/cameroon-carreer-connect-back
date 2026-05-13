package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobCompanyRepository extends JpaRepository<Company, UUID>, JpaSpecificationExecutor<Company> {

    /** Latest non-deleted company created by the given user, or empty. */
    java.util.Optional<Company> findFirstByCreatedByAndDeletedFalseOrderByCreatedAtDesc(UUID createdBy);

    /** All non-deleted companies created by the given user, newest first. */
    java.util.List<Company> findAllByCreatedByAndDeletedFalseOrderByCreatedAtDesc(UUID createdBy);
}
