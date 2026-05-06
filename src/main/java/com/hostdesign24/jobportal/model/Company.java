package com.hostdesign24.jobportal.model;

import com.hostdesign24.jobportal.model.enums.CompanySize;
import com.hostdesign24.jobportal.model.enums.CompanyStatus;
import com.hostdesign24.jobportal.model.enums.Industry;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Getter
@Setter
public class Company extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String website;

    @Enumerated(EnumType.STRING)
    private Industry industry;

    @Enumerated(EnumType.STRING)
    private CompanySize size;

    @ManyToOne
    @JoinColumn(name = "company_logo")
    private File logo;

    @Embedded
    private Address address;

    /**
     * Verification lifecycle. New companies start as PENDING and must be APPROVED
     * by a SYSTEM_ADMIN before recruiters can post jobs under them.
     *
     * The columnDefinition pushes a SQL DEFAULT so that schema migrations
     * adding this column to a table with pre-existing rows succeed without
     * a manual backfill.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyStatus status = CompanyStatus.PENDING;

    /**
     * Optional rejection reason set by the admin when status = REJECTED.
     */
    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    /**
     * Timestamp of the last verification decision (approved or rejected).
     */
    private LocalDateTime verifiedAt;
}
