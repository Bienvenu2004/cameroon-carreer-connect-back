package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.Skill;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID>,
        JpaSpecificationExecutor<Skill> {

    /**
     * Distinct skill names matching {@code q} (case-insensitive substring
     * match). Powers the frontend tag-autocomplete on the seeker profile
     * so users see existing skills as they type instead of inventing new
     * spellings of the same thing.
     *
     * Soft-deleted rows are excluded so the suggestion list stays clean.
     */
    @Query("SELECT DISTINCT s.name FROM Skill s " +
           "WHERE s.deleted = false " +
           "  AND s.name IS NOT NULL " +
           "  AND LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY s.name ASC")
    List<String> findDistinctNamesByQuery(@Param("q") String q, Pageable pageable);
}
