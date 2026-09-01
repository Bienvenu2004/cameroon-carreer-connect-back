package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.JobApplicationDto;
import com.hostdesign24.jobportal.dto.JobApplicationFilterDto;
import com.hostdesign24.jobportal.dto.JobSeekerApplyDto;
import com.hostdesign24.jobportal.dto.UpdateApplicationStatusDto;
import com.hostdesign24.jobportal.dto.ApplicationEventDto;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;

import java.util.List;
import java.util.UUID;

public interface JobSeekerApplyService {
    PageResponseDto<JobApplicationDto> getJobApplications(JobApplicationFilterDto filter);

    void addNew(JobSeekerApplyDto jobSeekerApply);

    void updateStatus(UUID applicationId, UpdateApplicationStatusDto request);

    /**
     * The candidate steps out of the pipeline. The only transition a seeker
     * controls -- someone who has taken another job should not sit in every
     * recruiter's shortlist indefinitely, wasting their time as much as theirs.
     */
    void withdraw(UUID applicationId, String reason);

    /** Full status history of one application, oldest first. */
    List<ApplicationEventDto> getTimeline(UUID applicationId);
}
