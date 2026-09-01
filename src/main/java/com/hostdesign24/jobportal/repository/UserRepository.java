package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    boolean existsByEmailAndDeletedFalse(String userEmail);

    Optional<User> findByEmailAndDeletedFalse(String email);

    Optional<User> findByIdAndDeletedFalse(UUID id);

    /* =====================================================================
     * Dashboard aggregates.
     *
     * These replace in-memory counting over findAll(). Each returns at most a
     * handful of rows and is computed by the database, so the admin dashboard
     * no longer loads the users table into the heap -- three times per request.
     *
     * Rows come back as Object[]{ groupingValue, count } which the service
     * folds into a map. Constructor expressions would be prettier but this
     * keeps the queries plain JPQL with no extra projection types.
     * ===================================================================== */

    @Query("SELECT u.role, COUNT(u) FROM User u WHERE u.deleted = false GROUP BY u.role")
    List<Object[]> countByRole();

    @Query("SELECT COUNT(u) FROM User u WHERE u.deleted = false AND u.isActive = true")
    long countActive();

    /**
     * Signup counts per calendar day since {@code from}. Bucketing into ISO weeks
     * happens in the service: the row count here is bounded by the number of days
     * in the window, not by the size of the users table.
     */
    @Query("SELECT u.registrationDate, COUNT(u) FROM User u "
            + "WHERE u.deleted = false AND u.registrationDate >= :from "
            + "GROUP BY u.registrationDate")
    List<Object[]> countSignupsByDaySince(@Param("from") LocalDate from);
}
