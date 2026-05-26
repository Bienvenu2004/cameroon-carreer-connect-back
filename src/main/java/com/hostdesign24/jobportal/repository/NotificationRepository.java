package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID>,
        JpaSpecificationExecutor<Notification> {

    /**
     * Paginated, newest-first list of notifications belonging to one user.
     * Soft-deleted notifications are excluded so the bell doesn't surface
     * messages the system has retired.
     */
    Page<Notification> findByRecipientUserIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID recipientUserId,
            Pageable pageable
    );

    /** Count of unread notifications for the bell badge. */
    long countByRecipientUserIdAndReadFalseAndDeletedFalse(UUID recipientUserId);

    /**
     * Bulk mark-all-as-read. Done via JPQL so we don't load each row into
     * the persistence context just to flip a boolean.
     */
    @Modifying
    @Query("UPDATE Notification n " +
           "   SET n.read = true, n.readAt = :now " +
           " WHERE n.recipientUserId = :userId AND n.read = false AND n.deleted = false")
    int markAllReadFor(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}
