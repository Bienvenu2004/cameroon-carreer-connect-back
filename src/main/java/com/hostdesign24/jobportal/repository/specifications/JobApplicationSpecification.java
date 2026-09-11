package com.hostdesign24.jobportal.repository.specifications;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.JobApplicationFilterDto;
import com.hostdesign24.jobportal.model.JobApplication;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.UserRole;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class JobApplicationSpecification {
    public Specification<JobApplication> build(JobApplicationFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Optional<User> currentOptUser = Utils.getCurrentUser();

            // Without a caller there is no scope to apply, and an unscoped
            // query would return every application. Match nothing instead.
            if (currentOptUser.isEmpty()) {
                return cb.disjunction();
            }

            currentOptUser.ifPresent(currentUser -> {
                if (currentUser.getRole() == UserRole.JOB_SEEKER){
                    predicates.add(cb.equal(root.get("createdBy"), currentUser.getId()));
                }

                if (currentUser.getRole() == UserRole.RECRUITER){
                    predicates.add(cb.equal(root.get("job").get("createdBy"), currentUser.getId()));
                }
            });

            if (filter.getProfileId() != null) {
                predicates.add(cb.equal(root.get("profile").get("id"), filter.getProfileId()));
            }

            if (filter.getJobId() != null) {
                predicates.add(cb.equal(root.get("job").get("id"), filter.getJobId()));
            }

            // ANDed with the role scoping above, so a notification link can
            // open one of the caller's own applications and nothing else.
            if (filter.getApplicationId() != null) {
                predicates.add(cb.equal(root.get("id"), filter.getApplicationId()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
