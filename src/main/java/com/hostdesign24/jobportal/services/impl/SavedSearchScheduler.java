package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.services.SavedSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic driver for saved-search alert emails. Runs every hour and
 * lets the service decide which searches are due based on each search's
 * own frequency setting.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SavedSearchScheduler {

    private final SavedSearchService savedSearchService;

    /** Run hourly. Cron: at minute 7 of every hour to spread load. */
    @Scheduled(cron = "0 7 * * * *")
    public void runHourly() {
        log.info("[saved-search] running scheduled alerts pass");
        savedSearchService.runAlerts();
    }
}
