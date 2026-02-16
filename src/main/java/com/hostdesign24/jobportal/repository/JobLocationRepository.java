package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.JobLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobLocationRepository extends JpaRepository<JobLocation, UUID> {
}
