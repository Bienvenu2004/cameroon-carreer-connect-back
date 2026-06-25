package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for AI-generated job recommendations.
 *
 * For Milestone 1 the only access pattern is "persist new" — query / history
 * endpoints come later. Kept slim deliberately; query methods land when
 * we have a UI that needs them.
 */
@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {
}
