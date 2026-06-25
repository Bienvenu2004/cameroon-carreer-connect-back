package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.SavedSearch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SavedSearchRepository extends JpaRepository<SavedSearch, UUID> {
    List<SavedSearch> findByProfileIdAndDeletedFalse(UUID profileId);

    List<SavedSearch> findByActiveTrueAndDeletedFalse();
}
