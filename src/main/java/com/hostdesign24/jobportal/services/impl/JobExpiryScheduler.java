package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.model.Job;
import com.hostdesign24.jobportal.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Closes job listings once their application deadline has passed.
 *
 * Before deadlines existed nothing on the platform ever aged out: a posting from
 * January sat in the listings looking exactly like one made this morning, and
 * candidates spent their evening and their data bundle applying to roles that
 * had been filled months earlier. On a board whose whole pitch is being the
 * trustworthy alternative to WhatsApp groups and notice boards, a wall of stale
 * listings is indistinguishable from a wall of fraudulent ones.
 *
 * The listing query already hides expired postings, so this is not what keeps
 * them out of search results — it is what makes the closure real and visible to
 * the recruiter, and what stops an expired job accepting applications through a
 * direct link.
 *
 * {@code closedByExpiry} records why the job was closed, mirroring
 * {@code closedByHire}: pushing the deadline out later reopens a job that lapsed,
 * but never one the recruiter closed by hand for some other reason.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobExpiryScheduler {

    private final JobRepository jobRepository;

    /**
     * Runs at 00:20 daily. Deadlines have day granularity, so hourly would be
     * twenty-three wasted passes; the offset keeps it clear of midnight jobs.
     */
    @Scheduled(cron = "0 20 0 * * *")
    @Transactional
    public void closeExpiredJobs() {
        List<Job> expired = jobRepository.findLapsed(LocalDate.now());
        if (expired.isEmpty()) {
            log.debug("[job-expiry] nothing to close");
            return;
        }

        for (Job job : expired) {
            job.setActive(false);
            job.setClosedByExpiry(true);
        }
        jobRepository.saveAll(expired);

        log.info("[job-expiry] closed {} listing(s) past their deadline", expired.size());
    }
}
