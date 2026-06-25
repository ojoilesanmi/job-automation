package com.jobagent.service;

import com.jobagent.dto.DashboardOverviewResponse;
import com.jobagent.dto.PipelineReportResponse;
import com.jobagent.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
}
