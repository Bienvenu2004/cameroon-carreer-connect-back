package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.dto.feed.ActivityEventDto;
import com.hostdesign24.jobportal.model.enums.ActivityType;
import com.hostdesign24.jobportal.model.enums.Region;
import com.hostdesign24.jobportal.repository.ActivityFeedRepository;
import com.hostdesign24.jobportal.services.ActivityFeedService;
import com.hostdesign24.jobportal.services.CompanyFollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Merges the derived event streams into one chronological feed.
 *
 * Each source is queried for its own most-recent {@code limit} rows and the
 * results are merged in memory. That is correct rather than lazy: taking the
 * newest N of a merged set can never require more than the newest N of any one
 * source. It also keeps three simple indexed queries instead of one UNION over
 * heterogeneous shapes that no index would serve well.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityFeedServiceImpl implements ActivityFeedService {

    /** Nobody scrolls a feed like this past here, and it bounds every query. */
    private static final int MAX_LIMIT = 50;

    private final ActivityFeedRepository activityFeedRepository;
    private final CompanyFollowService companyFollowService;

    @Override
    @Transactional(readOnly = true)
    public List<ActivityEventDto> platformFeed(int limit) {
        int capped = cap(limit);
        Pageable page = PageRequest.of(0, capped);

        List<ActivityEventDto> events = new ArrayList<>();
        collect(events, activityFeedRepository.recentJobPosts(page), ActivityType.JOB_POSTED);
        collect(events, activityFeedRepository.recentVerifications(page), ActivityType.COMPANY_VERIFIED);
        collect(events, activityFeedRepository.recentHires(page), ActivityType.POSITION_FILLED);

        return newestFirst(events, capped);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityEventDto> myFeed(int limit) {
        int capped = cap(limit);

        List<UUID> followed;
        try {
            followed = companyFollowService.myFollowedIds();
        } catch (RuntimeException e) {
            // Missing profile, or not a seeker: show the platform rather than an
            // error. This endpoint's whole job is to have something to say.
            log.debug("Falling back to the platform feed: {}", e.getMessage());
            return platformFeed(capped);
        }

        if (followed.isEmpty()) {
            return platformFeed(capped);
        }

        Pageable page = PageRequest.of(0, capped);
        List<ActivityEventDto> events = new ArrayList<>();
        collect(events, activityFeedRepository.recentJobPostsInCompanies(followed, page),
                ActivityType.JOB_POSTED);
        collect(events, activityFeedRepository.recentVerificationsInCompanies(followed, page),
                ActivityType.COMPANY_VERIFIED);
        collect(events, activityFeedRepository.recentHiresInCompanies(followed, page),
                ActivityType.POSITION_FILLED);

        return newestFirst(events, capped);
    }

    /** Rows are { jobId, jobTitle, companyId, companyName, region, occurredAt }. */
    private static void collect(List<ActivityEventDto> into, Collection<Object[]> rows, ActivityType type) {
        for (Object[] r : rows) {
            LocalDateTime at = (LocalDateTime) r[5];
            // A row with no timestamp cannot be placed in a chronological feed,
            // and inventing one would misrepresent when it happened.
            if (at == null) continue;

            into.add(ActivityEventDto.builder()
                    .type(type)
                    .jobId((UUID) r[0])
                    .jobTitle((String) r[1])
                    .companyId((UUID) r[2])
                    .companyName((String) r[3])
                    .region((Region) r[4])
                    .occurredAt(at)
                    .build());
        }
    }

    private static List<ActivityEventDto> newestFirst(List<ActivityEventDto> events, int limit) {
        return events.stream()
                .sorted(Comparator.comparing(ActivityEventDto::getOccurredAt).reversed())
                .limit(limit)
                .toList();
    }

    private static int cap(int limit) {
        if (limit < 1) return 10;
        return Math.min(limit, MAX_LIMIT);
    }
}
