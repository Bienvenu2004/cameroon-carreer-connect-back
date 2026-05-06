package com.hostdesign24.jobportal.repository.specifications;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobActivityFilterDto;
import com.hostdesign24.jobportal.model.Job;
import com.hostdesign24.jobportal.model.JobApplication;
import com.hostdesign24.jobportal.model.JobSave;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.UserRole;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Builds JPA Specifications for filtering {@link Job} entities.
 *
 * Field paths must match the Job entity exactly:
 *   - title (not jobTitle), description (not descriptionOfJob)
 *   - type (not jobType), site (not jobSite)
 *   - location (not jobLocation), company (not jobCompany)
 */
@Component
public class JobActivitySpecification {

    public Specification<Job> build(JobActivityFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Optional<User> currentOptUser = Utils.getCurrentUser();

            if (currentOptUser.isPresent()) {
                User currentUser = currentOptUser.get();

                if (currentUser.getRole() == UserRole.RECRUITER) {
                    // Recruiters only see jobs they themselves created.
                    predicates.add(cb.equal(root.get("createdBy"), currentUser.getId()));
                } else if (currentUser.getRole() == UserRole.JOB_SEEKER) {
                    includeSavedJobs(filter, root, query, cb, currentUser, predicates);
                }
                // SYSTEM_ADMIN sees everything (no ownership predicate).
            }

            if (filter.getJobTitle() != null && !filter.getJobTitle().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title").as(String.class)),
                        "%" + filter.getJobTitle().toLowerCase() + "%"));
            }

            if (filter.getCompanyName() != null && !filter.getCompanyName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("company").get("name").as(String.class)),
                        "%" + filter.getCompanyName().toLowerCase() + "%"));
            }

            if (filter.getCompanyCity() != null && !filter.getCompanyCity().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("location").get("city").as(String.class)),
                        "%" + filter.getCompanyCity().toLowerCase() + "%"));
            }

            if (filter.getCompanyState() != null && !filter.getCompanyState().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("location").get("stateRegion").as(String.class)),
                        "%" + filter.getCompanyState().toLowerCase() + "%"));
            }

            if (filter.getCompanyCountry() != null && !filter.getCompanyCountry().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("location").get("country").as(String.class)),
                        "%" + filter.getCompanyCountry().toLowerCase() + "%"));
            }

            if (filter.getRegion() != null) {
                predicates.add(cb.equal(root.get("location").get("region"), filter.getRegion()));
            }

            if (filter.getIndustry() != null) {
                predicates.add(cb.equal(root.get("company").get("industry"), filter.getIndustry()));
            }

            if (filter.getDescriptionOfJob() != null && !filter.getDescriptionOfJob().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("description").as(String.class)),
                        "%" + filter.getDescriptionOfJob().toLowerCase() + "%"));
            }

            if (filter.getJobType() != null) {
                predicates.add(cb.equal(root.get("type"), filter.getJobType()));
            }

            if (filter.getSalaryMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("salary"), filter.getSalaryMin()));
            }

            if (filter.getSalaryMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("salary"), filter.getSalaryMax()));
            }

            if (filter.getSalaryCurrency() != null) {
                predicates.add(cb.equal(root.get("salaryCurrency"), filter.getSalaryCurrency()));
            }

            if (filter.getJobSite() != null) {
                predicates.add(cb.equal(root.get("site"), filter.getJobSite()));
            }

            if (filter.getPostedDate() != null) {
                predicates.add(cb.equal(root.get("postedDate"), filter.getPostedDate()));
            }

            if (filter.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), filter.getIsActive()));
            }

            if (filter.getCreatedDaysAgo() != null) {
                LocalDate targetDate = LocalDate.now().minusDays(filter.getCreatedDaysAgo());
                predicates.add(cb.greaterThanOrEqualTo(root.get("postedDate"), targetDate));
            }

            // Hide soft-deleted rows by default.
            predicates.add(cb.equal(root.get("deleted"), false));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void includeSavedJobs(JobActivityFilterDto filter,
                                         Root<Job> root,
                                         CriteriaQuery<?> query,
                                         CriteriaBuilder cb,
                                         User currentUser,
                                         List<Predicate> predicates) {
        if (filter.getIsSaved() != null) {
            Subquery<UUID> savedSubquery = query.subquery(UUID.class);
            Root<JobSave> savedRoot = savedSubquery.from(JobSave.class);
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
