package com.hostdesign24.jobportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A job seeker following an employer.
 *
 * The point of this is not social display, it is the single highest-value
 * standing request a job seeker can make: "tell me when this employer is
 * hiring". Before it existed the only way to know was to check the listings by
 * hand, or to build a saved search that happened to match the company's jobs.
 *
 * Modelled as its own relation rather than as a {@link SavedSearch} with a
 * company filter. The two look similar in the database and are different to a
 * user: a saved search is a query someone composed and expects to manage, while
 * a follow is one click on a company page. Collapsing them would put "MTN
 * Cameroun" in a list of saved searches the seeker never created.
 *
 * Carries no "unfollow" flag: unfollowing soft-deletes the row, so the history
 * of who followed whom survives for the activity feed this is the first step
 * towards, without the row participating in notifications.
 */
@Entity
@Table(
        name = "company_follows",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_company_follow",
                columnNames = {"company_id", "profile_id"}))
@Getter
@Setter
public class CompanyFollow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id")
    private JobSeekerProfile profile;

    /**
     * Whether new jobs at this employer are emailed as well as shown in-app.
     *
     * Defaults to on. Someone who follows an employer has asked to hear about
     * them, and a job seeker is not sitting in the app waiting -- an in-app
     * notification alone would reach them days late. Separate from the follow
     * itself so it can be turned down without losing the follow.
     */
    @Column(nullable = false)
    private boolean emailAlerts = true;

    @Column(nullable = false)
    private LocalDateTime followedAt = LocalDateTime.now();
}
