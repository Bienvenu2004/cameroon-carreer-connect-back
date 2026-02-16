package com.hostdesign24.jobportal.repository.specifications;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.file.FileFilterDto;
import com.hostdesign24.jobportal.model.File;
import com.hostdesign24.jobportal.model.User;
import jakarta.persistence.criteria.Predicate;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class FileSpecification {


    public Specification<File> build(FileFilterDto filterDto) {

        User currentUser = getUser();

        UUID userId = currentUser.getId();

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.or(cb.equal(root.get("createdBy"), userId), cb.isMember(userId, root.get("destinedUserIds"))));

            if (filterDto.getName() != null) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + filterDto.getName().toLowerCase() + "%"));
            }
            if (filterDto.getType() != null) {
                predicates.add(cb.equal(root.get("type"), filterDto.getType()));
            }
            if (filterDto.getOwnerType() != null) {
                predicates.add(cb.equal(root.get("ownerType"), filterDto.getOwnerType()));
            }

            if (filterDto.getSizeFile() != null) {
                predicates.add(cb.equal(root.get("size"), filterDto.getSizeFile()));
            }

            if (filterDto.getRelatedEntity() != null) {
                predicates.add(cb.equal(root.get("relatedEntity"), filterDto.getRelatedEntity()));
            }

            if (filterDto.getOwnerId() != null) {
                predicates.add(cb.equal(root.get("ownerId"), filterDto.getOwnerId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static @NonNull User getUser() {
        return Utils.getCurrentUser()
                .orElseThrow(() -> new UsernameNotFoundException("Current user not found"));
    }
}
