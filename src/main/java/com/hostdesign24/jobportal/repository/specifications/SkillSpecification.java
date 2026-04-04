package com.hostdesign24.jobportal.repository.specifications;

import com.hostdesign24.jobportal.dto.skill.SkillFilterDto;
import com.hostdesign24.jobportal.model.Skill;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SkillSpecification {

    public Specification<Skill> build(SkillFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getName() != null && !filter.getName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + filter.getName().toLowerCase() + "%"));
            }

            if (filter.getExperienceLevel() != null && !filter.getExperienceLevel().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("experienceLevel")), filter.getExperienceLevel().toLowerCase()));
            }

            if (filter.getYearsOfExperience() != null && !filter.getYearsOfExperience().isBlank()) {
                predicates.add(cb.equal(root.get("yearsOfExperience"), filter.getYearsOfExperience()));
            }

            if (filter.getJobSeekerProfileId() != null) {
                predicates.add(cb.equal(root.get("jobSeekerProfile").get("id"), filter.getJobSeekerProfileId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

