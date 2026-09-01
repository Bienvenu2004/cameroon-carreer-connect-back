package com.hostdesign24.jobportal.services;

import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;

public interface NotificationAsyncService {

    @Async
    void notifyDeviceLogin(String email, String deviceName, String ipAddress);

    @Async
    void notifyNewApplication(String recruiterEmail, String candidateName, String jobTitle, String candidateEmail);

    @Async
    void notifyApplicationInterview(String seekerEmail, String seekerName, String jobTitle, String companyName,
                                    String interviewPlace, LocalDateTime interviewDateTime,
                                    String interviewPhone, String interviewNote);

    @Async
    void notifyApplicationHired(String seekerEmail, String seekerName, String jobTitle, String companyName);

    @Async
    /**
     * @param reason optional explanation from the recruiter, shown to the
     *               candidate. Null or blank renders the generic wording.
     */
    void notifyApplicationRejected(String seekerEmail, String seekerName, String jobTitle,
                                   String companyName, String reason);
}
