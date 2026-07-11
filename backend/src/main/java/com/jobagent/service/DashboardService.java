package com.jobagent.service;

import com.jobagent.dto.DashboardOverviewResponse;
import com.jobagent.dto.DetailedReportResponse;
import com.jobagent.dto.PipelineReportResponse;
import com.jobagent.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JobRepository jobRepository;
    private final JobMatchRepository matchRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview(UUID userId) {
        double avgScore = matchRepository.averageFitScoreByUserId(userId);

        return new DashboardOverviewResponse(
                jobRepository.count(),
                matchRepository.countByUserIdAndStatus(userId, "scored"),
                matchRepository.countByUserIdAndStatus(userId, "pending"),
                applicationRepository.countByUserIdAndStatus(userId, "submitted"),
                matchRepository.countByUserIdAndStatus(userId, "rejected"),
                applicationRepository.countByUserIdAndStatus(userId, "interview"),
                applicationRepository.countByUserIdAndStatus(userId, "assessment"),
                Math.round(avgScore * 100.0) / 100.0,
                Collections.emptyMap(),
                Collections.emptyMap()
        );
    }

    @Transactional(readOnly = true)
    public PipelineReportResponse getPipelineReport(UUID userId) {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        List<Object[]> statusRows = applicationRepository.countByStatusGrouped(userId);
        for (Object[] row : statusRows) {
            byStatus.put((String) row[0], (Long) row[1]);
        }

        List<PipelineReportResponse.CountryCount> byCountry = applicationRepository
                .countByCountryGrouped(userId).stream()
                .map(row -> new PipelineReportResponse.CountryCount((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());

        List<PipelineReportResponse.SourceCount> bySource = applicationRepository
                .countBySourceGrouped(userId).stream()
                .map(row -> new PipelineReportResponse.SourceCount(
                        row[0] != null ? row[0].toString() : "unknown",
                        (String) row[1],
                        (Long) row[2]))
                .collect(Collectors.toList());

        long total = applicationRepository.countByUserId(userId);
        long thisWeek = applicationRepository.countByUserIdAndSubmittedAtAfter(
                userId, OffsetDateTime.now().minusWeeks(1));
        long thisMonth = applicationRepository.countByUserIdAndSubmittedAtAfter(
                userId, OffsetDateTime.now().minusMonths(1));

        return new PipelineReportResponse(byStatus, byCountry, bySource, total, thisWeek, thisMonth);
    }

    @Transactional(readOnly = true)
    public DetailedReportResponse getDetailedReport(UUID userId) {
        long totalJobs = jobRepository.count();
        long totalMatches = matchRepository.countByUserId(userId);
        long totalApplications = applicationRepository.countByUserId(userId);
        long totalSubmitted = applicationRepository.countByUserIdAndStatus(userId, "submitted");
        long totalInterviews = applicationRepository.countByUserIdAndStatus(userId, "interview");
        long totalOffers = applicationRepository.countByUserIdAndStatus(userId, "offer");

        double averageMatchScore = matchRepository.averageFitScoreByUserId(userId);
        double responseRate = totalMatches > 0 ? (double) totalSubmitted / totalMatches * 100.0 : 0.0;

        Map<String, Long> matchesByStatus = new LinkedHashMap<>();
        List<Object[]> matchStatusRows = matchRepository.countByStatusGrouped(userId);
        for (Object[] row : matchStatusRows) {
            matchesByStatus.put((String) row[0], (Long) row[1]);
        }

        Map<String, Long> applicationsByStatus = new LinkedHashMap<>();
        List<Object[]> appStatusRows = applicationRepository.countByStatusGrouped(userId);
        for (Object[] row : appStatusRows) {
            applicationsByStatus.put((String) row[0], (Long) row[1]);
        }

        List<DetailedReportResponse.CountryReport> applicationsByCountry = applicationRepository
                .countByCountryGrouped(userId).stream()
                .map(row -> new DetailedReportResponse.CountryReport((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());

        List<DetailedReportResponse.SourceReport> applicationsBySource = applicationRepository
                .countBySourceGrouped(userId).stream()
                .map(row -> new DetailedReportResponse.SourceReport(
                        row[0] != null ? row[0].toString() : "unknown",
                        (Long) row[2]))
                .collect(Collectors.toList());

        List<DetailedReportResponse.WeeklyReport> weeklyTrend = buildWeeklyTrend(userId);

        return new DetailedReportResponse(
                totalJobs, totalMatches, totalApplications, totalSubmitted,
                totalInterviews, totalOffers,
                Math.round(averageMatchScore * 100.0) / 100.0,
                Math.round(responseRate * 100.0) / 100.0,
                matchesByStatus, applicationsByStatus,
                applicationsByCountry, applicationsBySource, weeklyTrend);
    }

    private List<DetailedReportResponse.WeeklyReport> buildWeeklyTrend(UUID userId) {
        List<DetailedReportResponse.WeeklyReport> trend = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (int i = 7; i >= 0; i--) {
            OffsetDateTime weekStart = now.minusWeeks(i).with(WeekFields.ISO.dayOfWeek(), 1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime weekEnd = weekStart.plusWeeks(1);

            long matches = matchRepository.countByUserIdAndDateRange(userId, weekStart, weekEnd);
            long applications = applicationRepository.countByUserIdAndDateRange(userId, weekStart, weekEnd);
            long submitted = applicationRepository.countByUserIdAndSubmittedAtBetween(userId, weekStart, weekEnd);

            String weekLabel = weekStart.toLocalDate().toString();
            trend.add(new DetailedReportResponse.WeeklyReport(weekLabel, matches, applications, submitted));
        }
        return trend;
    }
}
