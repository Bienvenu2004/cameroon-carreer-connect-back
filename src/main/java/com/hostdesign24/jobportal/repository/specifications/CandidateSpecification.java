package com.hostdesign24.jobportal.repository.specifications;

import com.hostdesign24.jobportal.dto.candidate.CandidateSearchFilterDto;
import com.hostdesign24.jobportal.model.Education;
import com.hostdesign24.jobportal.model.JobInvitation;
import com.hostdesign24.jobportal.model.JobSeekerProfile;
import com.hostdesign24.jobportal.model.Skill;
import com.hostdesign24.jobportal.model.WorkExperience;
import com.hostdesign24.jobportal.model.enums.DiplomaLevel;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds the recruiter-facing candidate search.
 *
 * Two invariants hold no matter what the caller asks for, and both are applied
 * before anything else:
 *
 *   1. {@code searchable = true}. Consent is not a filter the caller can relax.
 *      A profile appears here because its owner chose to be findable by
 *      employers, and no combination of query parameters can reach one that did
 *      not.
 *   2. {@code deleted = false}.
 *
 * Skills are matched with one correlated subquery per requested skill rather
 * than a join, so asking for "Java AND SQL" means the candidate holds both. A
 * join with an IN clause would return anyone holding either, which is a
 * different and much less useful question.
 */
@Component
public class CandidateSpecification {

    public Specification<JobSeekerProfile> build(CandidateSearchFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // --- non-negotiable ---
            predicates.add(cb.isTrue(root.get("searchable")));
            predicates.add(cb.isFalse(root.get("deleted")));

            if (filter.getRegion() != null) {
                predicates.add(cb.equal(root.get("address").get("region"), filter.getRegion()));
            }

            if (isSet(filter.getCity())) {
                predicates.add(cb.like(
                        cb.lower(root.get("address").get("city")),
                        "%" + filter.getCity().toLowerCase() + "%"));
            }

            if (isSet(filter.getLanguage())) {
                predicates.add(cb.like(
                        cb.lower(root.get("spokenLanguages")),
                        "%" + filter.getLanguage().toLowerCase() + "%"));
            }

            if (filter.getActiveWithinDays() != null && filter.getActiveWithinDays() > 0) {
                LocalDateTime cutoff = LocalDateTime.now().minusDays(filter.getActiveWithinDays());
                predicates.add(cb.greaterThanOrEqualTo(root.get("searchableSince"), cutoff));
            }

            // --- skills: every requested skill must be present ---
            if (filter.getSkills() != null && !filter.getSkills().isEmpty()) {
                for (String skill : filter.getSkills()) {
                    if (!isSet(skill)) continue;
                    Subquery<Long> sub = query.subquery(Long.class);
                    Root<Skill> s = sub.from(Skill.class);
                    sub.select(cb.literal(1L)).where(
                            cb.equal(s.get("jobSeekerProfile"), root),
                            cb.isFalse(s.get("deleted")),
                            cb.like(cb.lower(s.get("name")), "%" + skill.trim().toLowerCase() + "%"));
                    predicates.add(cb.exists(sub));
                }
            }

            // --- diploma: this level or better, by Bac+N rank ---
            if (filter.getMinimumDiploma() != null) {
                List<DiplomaLevel> acceptable = Arrays.stream(DiplomaLevel.values())
                        .filter(level -> level.satisfies(filter.getMinimumDiploma()))
                        .toList();
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Education> e = sub.from(Education.class);
                sub.select(cb.literal(1L)).where(
                        cb.equal(e.get("profile"), root),
                        cb.isFalse(e.get("deleted")),
                        e.get("level").in(acceptable));
                predicates.add(cb.exists(sub));
            }

            // --- keyword over roles held and subjects studied ---
            if (isSet(filter.getKeyword())) {
                String needle = "%" + filter.getKeyword().trim().toLowerCase() + "%";

                Subquery<Long> byRole = query.subquery(Long.class);
                Root<WorkExperience> w = byRole.from(WorkExperience.class);
                byRole.select(cb.literal(1L)).where(
                        cb.equal(w.get("profile"), root),
                        cb.isFalse(w.get("deleted")),
                        cb.like(cb.lower(w.get("title")), needle));

                Subquery<Long> byField = query.subquery(Long.class);
                Root<Education> e2 = byField.from(Education.class);
                byField.select(cb.literal(1L)).where(
                        cb.equal(e2.get("profile"), root),
                        cb.isFalse(e2.get("deleted")),
                        cb.like(cb.lower(e2.get("fieldOfStudy")), needle));

                predicates.add(cb.or(cb.exists(byRole), cb.exists(byField)));
            }

            // --- hide anyone already approached about this job ---
            if (filter.getExcludeInvitedForJobId() != null) {
                Subquery<Long> invited = query.subquery(Long.class);
                Root<JobInvitation> i = invited.from(JobInvitation.class);
                invited.select(cb.literal(1L)).where(
                        cb.equal(i.get("profile"), root),
                        cb.isFalse(i.get("deleted")),
                        cb.equal(i.get("job").get("id"), filter.getExcludeInvitedForJobId()));
                predicates.add(cb.not(cb.exists(invited)));
            }

            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
