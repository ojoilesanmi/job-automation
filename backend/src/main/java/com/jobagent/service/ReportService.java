package com.jobagent.service;

import com.jobagent.model.Application;
import com.jobagent.model.JobMatch;
import com.jobagent.model.User;
import com.jobagent.repository.ApplicationRepository;
import com.jobagent.repository.JobMatchRepository;
import com.jobagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ApplicationRepository applicationRepository;
    private final JobMatchRepository jobMatchRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public SummaryReport generateDailySummary(UUID userId) {
        long applicationsToday = applicationRepository.countTodaySubmissions(userId);

        OffsetDateTime todayStart = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime todayEnd = todayStart.plusDays(1);
        long matchesToday = jobMatchRepository.countByUserIdAndDateRange(userId, todayStart, todayEnd);

        List<Application> followUps = applicationRepository
                .findByUserIdAndNextFollowUpAtIsNotNullOrderByNextFollowUpAtAsc(userId, PageRequest.of(0, 10));
        long upcomingFollowUps = followUps.stream()
                .filter(a -> a.getNextFollowUpAt() != null && a.getNextFollowUpAt().isBefore(todayEnd))
                .count();

        long totalApplications = applicationRepository.countByUserId(userId);
        long totalMatches = jobMatchRepository.countByUserId(userId);

        SummaryReport report = new SummaryReport(
                applicationsToday,
                matchesToday,
                upcomingFollowUps,
                totalApplications,
                totalMatches
        );

        String message = String.format(
                "Daily Summary: %d applications today, %d new matches, %d upcoming follow-ups",
                applicationsToday, matchesToday, upcomingFollowUps);

        notificationService.createNotification(userId, "daily_summary", "Daily Summary", message,
                null, "report");

        log.info("Generated daily summary for user {}: {}", userId, message);
        return report;
    }

    @Transactional(readOnly = true)
    public SummaryReport generateWeeklySummary(UUID userId) {
        OffsetDateTime weekStart = OffsetDateTime.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime weekEnd = weekStart.plusWeeks(1);

        long applicationsThisWeek = applicationRepository.countByUserIdAndSubmittedAtBetween(userId, weekStart, weekEnd);
        long matchesThisWeek = jobMatchRepository.countByUserIdAndDateRange(userId, weekStart, weekEnd);

        long responded = applicationRepository.countByUserIdAndStatus(userId, "responded");
        long totalSubmitted = applicationRepository.countByUserId(userId);
        double responseRate = totalSubmitted > 0 ? (double) responded / totalSubmitted * 100 : 0;

        long totalApplications = applicationRepository.countByUserId(userId);
        long totalMatches = jobMatchRepository.countByUserId(userId);

        SummaryReport report = new SummaryReport(
                applicationsThisWeek,
                matchesThisWeek,
                responded,
                totalApplications,
                totalMatches
        );

        String message = String.format(
                "Weekly Summary: %d applications, %d new matches, %.1f%% response rate",
                applicationsThisWeek, matchesThisWeek, responseRate);

        notificationService.createNotification(userId, "weekly_summary", "Weekly Summary", message,
                null, "report");

        log.info("Generated weekly summary for user {}: {}", userId, message);
        return report;
    }

    @Scheduled(cron = "0 0 20 * * *")
    @Transactional
    public void runDailySummaries() {
        log.info("Running scheduled daily summaries for all users");
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                generateDailySummary(user.getId());
            } catch (Exception e) {
                log.error("Failed to generate daily summary for user {}: {}", user.getId(), e.getMessage());
            }
        }
        log.info("Completed daily summaries for {} users", users.size());
    }

    @Scheduled(cron = "0 0 9 * * 1")
    @Transactional
    public void runWeeklySummaries() {
        log.info("Running scheduled weekly summaries for all users");
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                generateWeeklySummary(user.getId());
            } catch (Exception e) {
                log.error("Failed to generate weekly summary for user {}: {}", user.getId(), e.getMessage());
            }
        }
        log.info("Completed weekly summaries for {} users", users.size());
    }

    public record SummaryReport(
            long applicationsCount,
            long matchesCount,
            long followUpsCount,
            long totalApplications,
            long totalMatches
    ) {}
}
