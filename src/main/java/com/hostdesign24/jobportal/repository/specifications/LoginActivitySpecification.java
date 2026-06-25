package com.hostdesign24.jobportal.repository.specifications;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.userDevice.LoginActivityFilterDto;
import com.hostdesign24.jobportal.model.LoginActivity;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.UserRole;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class LoginActivitySpecification {

    private User getCurrentUser() {
        return Utils.getCurrentUser().orElseThrow(() -> new SecurityException("User not authenticated"));
    }

    private boolean isSystemAdmin(User user) {
        return user.getRole() == UserRole.SYSTEM_ADMIN;
    }

    private Date getStartOfDay(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date getEndOfDay(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Date.from(localDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());
    }

    public Specification<LoginActivity> build(LoginActivityFilterDto filterDto) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            User currentUser = getCurrentUser();

            applyUserFilter(predicates, criteriaBuilder, root, currentUser, filterDto);
            applyIpFilter(predicates, criteriaBuilder, root, filterDto);
            applySuccessFilter(predicates, root, filterDto);
            applyDeviceFilter(predicates, root, filterDto);
            applyDateFilter(predicates, criteriaBuilder, root, filterDto);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void applyUserFilter(List<Predicate> predicates,
                                 CriteriaBuilder cb,
                                 Root<LoginActivity> root,
                                 User currentUser,
                                 LoginActivityFilterDto filterDto) {
        if (!isSystemAdmin(currentUser)) {
            predicates.add(cb.equal(root.get("userId"), currentUser.getId()));
        } else if (filterDto.getUserId() != null) {
            predicates.add(cb.equal(root.get("userId"), filterDto.getUserId()));
        }
    }

    private void applyIpFilter(List<Predicate> predicates,
                               CriteriaBuilder cb,
                               Root<LoginActivity> root,
                               LoginActivityFilterDto filterDto) {
        String ip = filterDto.getIpAddress();
        if (ip != null) {
            ip = ip.trim();
            if (!ip.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("ipAddress")), "%" + ip.toLowerCase() + "%"));
            }
        }
    }

    private void applySuccessFilter(List<Predicate> predicates,
                                    Root<LoginActivity> root,
                                    LoginActivityFilterDto filterDto) {
        if (filterDto.getSuccessful() != null) {
            predicates.add(root.get("successful").in(filterDto.getSuccessful()));
        }
    }

    private void applyDeviceFilter(List<Predicate> predicates,
                                   Root<LoginActivity> root,
                                   LoginActivityFilterDto filterDto) {
        if (filterDto.getDeviceId() != null) {
            predicates.add(root.get("deviceId").in(filterDto.getDeviceId()));
        }
    }

    private void applyDateFilter(List<Predicate> predicates,
                                 CriteriaBuilder cb,
                                 Root<LoginActivity> root,
                                 LoginActivityFilterDto filterDto) {
        if (filterDto.getDate() != null) {
            Date startOfDay = getStartOfDay(filterDto.getDate());
            Date endOfDay = getEndOfDay(filterDto.getDate());
            predicates.add(cb.between(root.get("createdAt"), startOfDay, endOfDay));
        } else if (filterDto.getBetweenStartDate() != null) {
            Date endDate = filterDto.getBetweenEndDate() != null ? filterDto.getBetweenEndDate() : new Date();
            predicates.add(cb.between(root.get("createdAt"), filterDto.getBetweenStartDate(), endDate));
        }
    }
}