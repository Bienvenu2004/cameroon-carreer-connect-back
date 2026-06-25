package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.savedsearch.SavedSearchDto;
import com.hostdesign24.jobportal.exception.ActionDeniedException;
import com.hostdesign24.jobportal.exception.ResourceNotFoundException;
import com.hostdesign24.jobportal.model.Job;
import com.hostdesign24.jobportal.model.JobSeekerProfile;
import com.hostdesign24.jobportal.model.SavedSearch;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.SavedSearchFrequency;
import com.hostdesign24.jobportal.model.enums.UserRole;
import com.hostdesign24.jobportal.repository.JobRepository;
import com.hostdesign24.jobportal.repository.JobSeekerProfileRepository;
import com.hostdesign24.jobportal.repository.SavedSearchRepository;
import com.hostdesign24.jobportal.services.EmailService;
import com.hostdesign24.jobportal.services.SavedSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SavedSearchServiceImpl implements SavedSearchService {

    private final SavedSearchRepository repository;
    private final JobSeekerProfileRepository profileRepository;
    private final JobRepository jobRepository;
    private final EmailService emailService;

    @Value("${app.client-url}")
    private String clientUrl;

    @Override
    @Transactional
    public SavedSearchDto create(SavedSearchDto dto) {
        JobSeekerProfile profile = currentSeekerProfileOrThrow();
        SavedSearch entity = new SavedSearch();
        applyDto(dto, entity);
        entity.setProfile(profile);
        entity = repository.save(entity);
        return toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedSearchDto> listMine() {
        JobSeekerProfile profile = currentSeekerProfileOrThrow();
        return repository.findByProfileIdAndDeletedFalse(profile.getId())
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public SavedSearchDto update(UUID id, SavedSearchDto dto) {
        SavedSearch entity = ownedOrThrow(id);
        applyDto(dto, entity);
        return toDto(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        SavedSearch entity = ownedOrThrow(id);
        entity.setDeleted(true);
        entity.setActive(false);
        repository.save(entity);
    }

    @Override
    @Transactional
    public void runAlerts() {
        LocalDateTime now = LocalDateTime.now();
        List<SavedSearch> active = repository.findByActiveTrueAndDeletedFalse();
        for (SavedSearch s : active) {
            try {
                if (!isDue(s, now)) continue;
                List<Job> matches = findMatches(s);
                if (matches.isEmpty()) {
                    s.setLastSentAt(now);
                    repository.save(s);
                    continue;
                }
                sendAlertEmail(s, matches);
                s.setLastSentAt(now);
                repository.save(s);
            } catch (Exception ex) {
                log.warn("Saved-search alert failed for id={}: {}", s.getId(), ex.getMessage());
            }
        }
    }

    /* ---------------- helpers ---------------- */

    private JobSeekerProfile currentSeekerProfileOrThrow() {
        User user = Utils.getCurrentUser()
                .orElseThrow(() -> new ActionDeniedException("Authentication required"));
        if (user.getRole() != UserRole.JOB_SEEKER) {
            throw new ActionDeniedException("Only job seekers can manage saved searches");
        }
        JobSeekerProfile p = user.getJobSeekerProfile();
        if (p == null) {
            // load via repository (lazy may not be initialized)
            p = profileRepository.findByUserId(user.getId());
            if (p == null) {
                throw new ResourceNotFoundException("Job seeker profile not found");
            }
        }
        return p;
    }

    private SavedSearch ownedOrThrow(UUID id) {
        SavedSearch s = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Saved search not found: " + id));
        JobSeekerProfile profile = currentSeekerProfileOrThrow();
        if (!s.getProfile().getId().equals(profile.getId())) {
            throw new ActionDeniedException("You don't own this saved search");
        }
        return s;
    }

    private void applyDto(SavedSearchDto dto, SavedSearch entity) {
        if (dto.getLabel() != null) entity.setLabel(dto.getLabel());
        entity.setKeyword(dto.getKeyword());
        entity.setRegion(dto.getRegion());
        entity.setIndustry(dto.getIndustry());
        entity.setJobType(dto.getJobType());
        entity.setJobSite(dto.getJobSite());
        entity.setSalaryMin(dto.getSalaryMin());
        entity.setSalaryMax(dto.getSalaryMax());
        if (dto.getSalaryCurrency() != null) entity.setSalaryCurrency(dto.getSalaryCurrency());
        entity.setActive(dto.isActive());
        if (dto.getFrequency() != null) entity.setFrequency(dto.getFrequency());
    }

    private SavedSearchDto toDto(SavedSearch s) {
        SavedSearchDto d = new SavedSearchDto();
        d.setId(s.getId());
        d.setLabel(s.getLabel());
        d.setKeyword(s.getKeyword());
        d.setRegion(s.getRegion());
        d.setIndustry(s.getIndustry());
        d.setJobType(s.getJobType());
        d.setJobSite(s.getJobSite());
        d.setSalaryMin(s.getSalaryMin());
        d.setSalaryMax(s.getSalaryMax());
        d.setSalaryCurrency(s.getSalaryCurrency());
        d.setActive(s.isActive());
        d.setFrequency(s.getFrequency());
        d.setLastSentAt(s.getLastSentAt());
        d.setCreatedAt(s.getCreatedAt());
        return d;
    }

    private boolean isDue(SavedSearch s, LocalDateTime now) {
        LocalDateTime last = s.getLastSentAt();
        if (last == null) return true;
        return switch (s.getFrequency() == null ? SavedSearchFrequency.DAILY : s.getFrequency()) {
            case DAILY -> last.plusDays(1).isBefore(now);
            case WEEKLY -> last.plusWeeks(1).isBefore(now);
        };
    }

    private List<Job> findMatches(SavedSearch s) {
        LocalDateTime since = s.getLastSentAt() != null
                ? s.getLastSentAt()
                : LocalDateTime.now().minusDays(7);

        return jobRepository.findAll().stream()
                .filter(j -> !j.isDeleted() && j.isActive())
                .filter(j -> j.getCreatedAt() != null && j.getCreatedAt().isAfter(since))
                .filter(j -> matches(s, j))
                .toList();
    }

    private boolean matches(SavedSearch s, Job j) {
        if (s.getKeyword() != null && !s.getKeyword().isBlank()) {
            String kw = s.getKeyword().toLowerCase();
            String title = j.getTitle() == null ? "" : j.getTitle().toLowerCase();
            String desc = j.getDescription() == null ? "" : j.getDescription().toLowerCase();
            if (!title.contains(kw) && !desc.contains(kw)) return false;
        }
        if (s.getRegion() != null
                && (j.getLocation() == null || j.getLocation().getRegion() != s.getRegion())) return false;
        if (s.getIndustry() != null
                && (j.getCompany() == null || j.getCompany().getIndustry() != s.getIndustry())) return false;
        if (s.getJobType() != null && j.getType() != s.getJobType()) return false;
        if (s.getJobSite() != null && j.getSite() != s.getJobSite()) return false;
        BigDecimal salary = j.getSalary();
        if (s.getSalaryMin() != null && (salary == null || salary.compareTo(s.getSalaryMin()) < 0)) return false;
        if (s.getSalaryMax() != null && (salary == null || salary.compareTo(s.getSalaryMax()) > 0)) return false;
        return true;
    }

    private void sendAlertEmail(SavedSearch s, List<Job> matches) {
        String email = s.getProfile().getUser().getEmail();
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("label", s.getLabel());
        ctx.put("count", matches.size());
        ctx.put("clientUrl", clientUrl);
        ctx.put("jobs", matches.stream().limit(10).map(j -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", j.getId());
            m.put("title", j.getTitle());
            m.put("company", j.getCompany() == null ? "" : j.getCompany().getName());
            m.put("city", j.getLocation() == null ? "" : j.getLocation().getCity());
            m.put("region", j.getLocation() == null || j.getLocation().getRegion() == null
                    ? "" : j.getLocation().getRegion().getDisplayName());
            return m;
        }).toList());

        String subject = matches.size() + " new job(s) match your saved search: " + s.getLabel();
        emailService.sendEmail(email, subject, "saved-search-alert", ctx);
    }
}
