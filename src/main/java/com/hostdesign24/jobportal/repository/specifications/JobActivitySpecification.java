package com.hostdesign24.jobportal.repository.specifications;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobActivityFilterDto;
import com.hostdesign24.jobportal.model.JobPost;
import com.hostdesign24.jobportal.model.JobApplication;
import com.hostdesign24.jobportal.model.JobSeekerSave;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.UserRole;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JobActivitySpecification {
    public Specification<JobPost> build(JobActivityFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Optional<User> currentOptUser = Utils.getCurrentUser();

            if (currentOptUser.isPresent()){
                User currentUser = currentOptUser.get();

                if (currentUser.getRole() == UserRole.RECRUITER){
                    predicates.add(cb.equal(root.get("createdBy"), currentUser.getId()));
                } else {
                    includeSavedJobs(filter, root, query, cb, currentUser, predicates);
                    Subquery<UUID> sub = query.subquery(UUID.class);
                    Root<JobApplication> subRoot = sub.from(JobApplication.class);
                    sub.select(subRoot.get("job").get("id"));  // Select job ID from JobSeekerApply
                    sub.where(
                            cb.equal(subRoot.get("job").get("id"), root.get("id")),  // Compare job IDs
                            cb.equal(subRoot.get("profile").get("user").get("id"), currentUser.getId())
                    );
                    predicates.add(cb.exists(sub));

                }
            }
            if (filter.getJobTitle() != null && !filter.getJobTitle().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("jobTitle").as(String.class)),
                        "%" + filter.getJobTitle().toLowerCase() + "%"));
            }

            if (filter.getCompanyName() != null && !filter.getCompanyName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("jobCompany").get("name").as(String.class)),
                        "%" + filter.getCompanyName().toLowerCase() + "%"));
            }

            if (filter.getCompanyCity() != null && !filter.getCompanyCity().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("jobLocation").get("city").as(String.class)),
                        "%" + filter.getCompanyCity().toLowerCase() + "%"));
            }

            if (filter.getCompanyState() != null && !filter.getCompanyState().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("jobLocation").get("state").as(String.class)),
                        "%" + filter.getCompanyState().toLowerCase() + "%"));
            }

            if (filter.getCompanyCountry() != null && !filter.getCompanyCountry().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("jobLocation").get("country").as(String.class)),
                        "%" + filter.getCompanyCountry().toLowerCase() + "%"));
            }

            if (filter.getDescriptionOfJob() != null && !filter.getDescriptionOfJob().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("descriptionOfJob").as(String.class)),
                        "%" + filter.getDescriptionOfJob().toLowerCase() + "%"));
            }

            if (filter.getJobType() != null) {
                predicates.add(cb.equal(root.get("jobType"), filter.getJobType()));
            }

            if (filter.getSalary() != null) {
                predicates.add(cb.equal(root.get("salary"), filter.getSalary()));
            }

            if (filter.getSalaryCurrency() != null) {
                predicates.add(cb.equal(root.get("salaryCurrency"), filter.getSalaryCurrency()));
            }

            if (filter.getJobSite() != null) {
                predicates.add(cb.equal(root.get("jobSite"), filter.getJobSite()));
            }

            if (filter.getPostedDate() != null) {
                predicates.add(cb.equal(root.get("postedDate"), filter.getPostedDate()));
            }

            // FIXED: Changed from filter.isActive() to check if the filter explicitly sets active status
            if (filter.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), filter.getIsActive()));
            }

            if (filter.getCreatedDaysAgo() != null) {
                LocalDate targetDate = LocalDate.now().minusDays(filter.getCreatedDaysAgo());
                predicates.add(cb.greaterThanOrEqualTo(root.get("postedDate"), targetDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void includeSavedJobs(JobActivityFilterDto filter, Root<JobPost> root, CriteriaQuery<?> query, CriteriaBuilder cb, User currentUser, List<Predicate> predicates) {
        if (filter.getIsSaved() != null) {
            Subquery<UUID> savedSubquery = query.subquery(UUID.class);
            Root<JobSeekerSave> savedRoot = savedSubquery.from(JobSeekerSave.class);
            savedSubquery.select(savedRoot.get("id"));
            savedSubquery.where(
                    cb.equal(savedRoot.get("job").get("id"), root.get("id")),
                    cb.equal(savedRoot.get("profile").get("user").get("id"), currentUser.getId())
            );

            if (filter.getIsSaved()) {
                predicates.add(cb.exists(savedSubquery));
            } else {
                predicates.add(cb.not(cb.exists(savedSubquery)));
            }
        }
    }
}