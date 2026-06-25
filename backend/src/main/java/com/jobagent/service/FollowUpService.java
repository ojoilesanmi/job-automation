package com.jobagent.service;

import com.jobagent.dto.FollowUpResponse;
import com.jobagent.model.Application;
import com.jobagent.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpService {

    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    @Transactional
    public FollowUpResponse scheduleFollowUp(UUID userId, UUID applicationId, OffsetDateTime nextFollowUpAt) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new com.jobagent.exception.ResourceNotFoundException("Application not found"));
        if (!app.getUser().getId().equals(userId)) {
            throw new com.jobagent.exception.ForbiddenException("Access denied");
        }
        app.setNextFollowUpAt(nextFollowUpAt);
        applicationRepository.save(app);
        return new FollowUpResponse(app.getId(), app.getNextFollowUpAt(), app.getLastFollowUpAt());
    }

    @Transactional(readOnly = true)
    public List<FollowUpResponse> getUpcomingFollowUps(UUID userId) {
        List<Application> apps = applicationRepository
                .findByUserIdAndNextFollowUpAtIsNotNullOrderByNextFollowUpAtAsc(userId, PageRequest.of(0, 20));
        return apps.stream()
                .map(a -> new FollowUpResponse(a.getId(), a.getNextFollowUpAt(), a.getLastFollowUpAt()))
                .collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void checkFollowUps() {
        List<Application> dueApps = applicationRepository
                .findByNextFollowUpAtBefore(OffsetDateTime.now());
        for (Application app : dueApps) {
            notificationService.createNotification(
                    app.getUser().getId(),
                    "follow_up_due",
                    "Follow-up due",
                    "Time to follow up on your application for " + app.getJob().getTitle() + " at " + app.getJob().getCompany(),
                    app.getId(),
                    "application"
            );
            app.setLastFollowUpAt(OffsetDateTime.now());
            app.setNextFollowUpAt(null);
            applicationRepository.save(app);
        }
        if (!dueApps.isEmpty()) {
            log.info("Processed {} follow-up notifications", dueApps.size());
        }
    }
}
