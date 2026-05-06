package com.hostdesign24.jobportal.repository.specifications;

import com.hostdesign24.jobportal.dto.admin.AdminUserFilterDto;
import com.hostdesign24.jobportal.model.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AdminUserSpecification {

    public Specification<User> build(AdminUserFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getRole() != null) {
                predicates.add(cb.equal(root.get("role"), filter.getRole()));
            }

            if (filter.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), filter.getActive()));
            }

            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("email")),
                        "%" + filter.getSearch().toLowerCase() + "%"));
            }

            // Hide soft-deleted unless explicitly searched.
            predicates.add(cb.equal(root.get("deleted"), false));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
