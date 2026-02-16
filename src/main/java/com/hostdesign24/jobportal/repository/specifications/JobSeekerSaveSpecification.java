package com.hostdesign24.jobportal.repository.specifications;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.JobApplicationFilterDto;
import com.hostdesign24.jobportal.dto.JobSeekerSaveFilter;
import com.hostdesign24.jobportal.model.JobApplication;
import com.hostdesign24.jobportal.model.JobSeekerSave;
import com.hostdesign24.jobportal.model.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JobSeekerSaveSpecification {
    public Specification<JobSeekerSave> build(JobSeekerSaveFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            User currentUser = Utils.getCurrentUser().orElseThrow(
                    () -> new UsernameNotFoundException("User not authenticated")
            );

            predicates.add(cb.equal(root.get("createdBy"), currentUser.getId()));

            if (filter.getProfileId() != null) {
                predicates.add(cb.equal(root.get("profile").get("id"), filter.getProfileId()));
            }

            if (filter.getJobId() != null) {
                predicates.add(cb.equal(root.get("job").get("id"), filter.getJobId()));
            }

            if (filter.getSavedOn() != null){
                predicates.add(cb.equal(root.get("savedOn"), filter.getSavedOn()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
