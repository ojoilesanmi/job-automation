package com.jobagent.service;

import com.jobagent.dto.GenerateCoverLetterRequest;
import com.jobagent.model.*;
import com.jobagent.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoApplyService {

    private final JobMatchRepository matchRepository;
    private final ApplicationRepository applicationRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final CoverLetterService coverLetterService;
    private final NotificationService notificationService;

    @Value("${app.auto-apply.enabled:false}")
    private boolean autoApplyEnabled;

    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void processAutoApply() {
        if (!autoApplyEnabled) {
            log.debug("Auto-apply skipped because app.auto-apply.enabled=false");
            return;
        }
        List<UserPreferences> allPrefs = preferencesRepository.findAll();
        for (UserPreferences prefs : allPrefs) {
            if (Boolean.FALSE.equals(prefs.getApprovalRequired())) {
                processAutoApplyForUser(prefs.getUser().getId());
            }
        }
    }

    @Transactional
    public Map<String, Object> processAutoApplyForUser(UUID userId) {
        if (!autoApplyEnabled) {
            return Map.of("autoApplyEnabled", false, "processed", 0, "skipped", 0);
        }
        UserPreferences prefs = preferencesRepository.findByUserId(userId).orElse(null);
        if (prefs == null || Boolean.TRUE.equals(prefs.getApprovalRequired())) {
            return Map.of("autoApplyEnabled", false, "processed", 0);
        }

        List<JobMatch> candidates = matchRepository.findByUserIdAndStatusOrderByFitScoreDesc(userId, "scored", null)
                .getContent();
        int processed = 0;
        int skipped = 0;

        for (JobMatch match : candidates) {
            int dailyLimit = prefs.getMaxApplicationsPerDay() != null ? prefs.getMaxApplicationsPerDay() : 0;
            if (dailyLimit <= 0 || processed >= dailyLimit) break;

            boolean meetsThreshold = match.getFitScore().compareTo(prefs.getMinimumRemoteFitScore()) >= 0;
            boolean notExcluded = prefs.getExcludedCompanies() == null ||
                    !prefs.getExcludedCompanies().toLowerCase()
                            .contains(match.getJob().getCompany().toLowerCase());

            if (meetsThreshold && notExcluded) {
                Optional<Application> existing = applicationRepository.findByUserIdAndJobId(userId, match.getJob().getId());
                if (existing.isEmpty()) {
                    try {
                        coverLetterService.generateCoverLetter(userId,
                                new GenerateCoverLetterRequest(match.getJob().getId(), null, null));

                        Application app = Application.builder()
                                .user(buildUser(userId))
                                .job(match.getJob())
                                .status("approved")
                                .applicationMode("auto_apply")
                                .build();
                        applicationRepository.save(app);
                        match.setStatus("auto_applied");
                        matchRepository.save(match);
                        processed++;
                    } catch (Exception e) {
                        log.warn("Auto-apply failed for match {}: {}", match.getId(), e.getMessage());
                        skipped++;
                    }
                }
            } else {
                skipped++;
            }
        }

        if (processed > 0) {
            notificationService.createNotification(userId, "auto_apply",
                    "Auto-apply completed",
                    processed + " applications auto-approved, " + skipped + " skipped",
                    null, "application");
        }

        return Map.of("processed", processed, "skipped", skipped);
    }

    private User buildUser(UUID userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }
}
