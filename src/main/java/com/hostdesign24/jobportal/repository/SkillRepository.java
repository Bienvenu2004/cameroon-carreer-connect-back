package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID>,
        JpaSpecificationExecutor<Skill> {
}
