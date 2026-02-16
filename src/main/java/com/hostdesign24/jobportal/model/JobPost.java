package com.hostdesign24.jobportal.model;

import com.hostdesign24.jobportal.model.enums.JobSite;
import com.hostdesign24.jobportal.model.enums.JobType;
import com.hostdesign24.jobportal.model.enums.SalaryCurrency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = {"jobLocation", "jobCompany"})
public class JobPost extends BaseEntity {

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "location_id")
    private JobLocation location;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "company_id")
    private JobCompany company;

    private boolean isActive = true;

    private boolean isSaved = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private JobType type;

    private BigDecimal salary;

    @Enumerated(EnumType.STRING)
    private SalaryCurrency salaryCurrency = SalaryCurrency.XAF;

    @Enumerated(EnumType.STRING)
    private JobSite site;
    private LocalDate postedDate = LocalDate.now();
    private String title;
}