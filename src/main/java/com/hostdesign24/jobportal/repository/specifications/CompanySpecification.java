package com.hostdesign24.jobportal.repository.specifications;

import com.hostdesign24.jobportal.dto.company.CompanyFilterDto;
import com.hostdesign24.jobportal.model.Company;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CompanySpecification {

    public Specification<Company> build(CompanyFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getName() != null && !filter.getName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + filter.getName().toLowerCase() + "%"));
            }

            if (filter.getCity() != null && !filter.getCity().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("address").get("city")), "%" + filter.getCity().toLowerCase() + "%"));
            }

            if (filter.getStateRegion() != null && !filter.getStateRegion().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("address").get("stateRegion")), "%" + filter.getStateRegion().toLowerCase() + "%"));
            }

            if (filter.getRegion() != null) {
                predicates.add(cb.equal(root.get("address").get("region"), filter.getRegion()));
            }

            if (filter.getCountry() != null && !filter.getCountry().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("address").get("country")), "%" + filter.getCountry().toLowerCase() + "%"));
            }

            if (filter.getIndustry() != null) {
                predicates.add(cb.equal(root.get("industry"), filter.getIndustry()));
            }

            if (filter.getCompanySize() != null) {
                predicates.add(cb.equal(root.get("size"), filter.getCompanySize()));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getLogoId() != null) {
                predicates.add(cb.equal(root.get("logo").get("id"), filter.getLogoId()));
            }

            // Hide soft-deleted by default.
            predicates.add(cb.equal(root.get("deleted"), false));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
