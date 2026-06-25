package com.hostdesign24.jobportal.repository.specifications;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.userDevice.UserDeviceFilterDto;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.UserDevice;
import com.hostdesign24.jobportal.model.enums.UserRole;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
public class DeviceSpecification {

    private User getCurrentUser() {
        return Utils.getCurrentUser()
                .orElseThrow(() -> new SecurityException("User not authenticated"));
    }

    private boolean isSystemAdmin(User user) {
        return user.getRole() == UserRole.SYSTEM_ADMIN;
    }

    public Specification<UserDevice> build(UserDeviceFilterDto filterDto) {
        final UserDeviceFilterDto filter = filterDto == null ? new UserDeviceFilterDto() : filterDto;

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            User currentUser = getCurrentUser();

            applyUserFilter(predicates, criteriaBuilder, root, currentUser, filter);

            String deviceName = trimToNull(filter.getDeviceName());
            addLikePredicateIfNotNull(predicates, criteriaBuilder, root, "deviceName", deviceName);

            String ipAddress = trimToNull(filter.getIpAddress());
            addLikePredicateIfNotNull(predicates, criteriaBuilder, root, "ipAddress", ipAddress);

            String userAgent = trimToNull(filter.getUserAgent());
            addLikePredicateIfNotNull(predicates, criteriaBuilder, root, "userAgent", userAgent);

            if (filter.getVerified() != null) {
                predicates.add(criteriaBuilder.equal(root.get("verified"), filter.getVerified()));
            }

            if (filter.getDeviceStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("deviceStatus"), filter.getDeviceStatus()));
            }

            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void applyUserFilter(List<Predicate> predicates,
                                 CriteriaBuilder criteriaBuilder,
                                 Root<UserDevice> root,
                                 User currentUser,
                                 UserDeviceFilterDto filterDto) {
        if (!isSystemAdmin(currentUser)) {
            predicates.add(criteriaBuilder.equal(root.get("userId"), currentUser.getId()));
            return;
        }

        if (filterDto.getUserId() != null) {
            predicates.add(criteriaBuilder.equal(root.get("userId"), filterDto.getUserId()));
        }
    }

    private void addLikePredicateIfNotNull(List<Predicate> predicates,
                                           CriteriaBuilder cb,
                                           Root<UserDevice> root,
                                           String field,
                                           String value) {
        if (value != null) {
            predicates.add(cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
        }
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}