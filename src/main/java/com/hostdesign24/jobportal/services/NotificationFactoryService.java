package com.hostdesign24.jobportal.services;


import com.hostdesign24.jobportal.dto.NotificationRequestDto;

import java.util.UUID;

public interface NotificationFactoryService {

    NotificationRequestDto createAccountCreationNotification(UUID userId);

    NotificationRequestDto createPasswordResetNotification(UUID userId);

    NotificationRequestDto userNewConnection(UUID userId, String device);

    NotificationRequestDto newJobApplication(UUID recruiterId, String candidateName, String jobTitle, UUID applicationId);

    NotificationRequestDto applicationStatusChanged(UUID jobSeekerId, String jobTitle, String newStatus, UUID applicationId);

    /** A recruiter has asked this candidate to apply for a specific job. */
    NotificationRequestDto jobInvitation(UUID jobSeekerId, String jobTitle, String companyName, UUID jobId);

    /** An employer the seeker follows has posted a job. */
    NotificationRequestDto newJobAtFollowedCompany(UUID jobSeekerId, String companyName,
                                                  String jobTitle, UUID jobId);

    /** An administrator has acted on a job the user reported. */
    NotificationRequestDto jobReportResolved(UUID reporterId, String jobTitle, boolean upheld, UUID jobId);
}
