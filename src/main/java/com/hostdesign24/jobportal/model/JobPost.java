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

    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    private boolean isSaved = false;

    @Builder.Default
    private Integer views = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private JobType type;

    private BigDecimal salary;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SalaryCurrency salaryCurrency = SalaryCurrency.XAF;

    @Enumerated(EnumType.STRING)
    private JobSite site;
    @Builder.Default
    private LocalDate postedDate = LocalDate.now();
    private String title;

    @Column(columnDefinition = "TEXT")
    private String benefits;
}