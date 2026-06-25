package com.jobagent.service;

import com.jobagent.dto.DashboardOverviewResponse;
import com.jobagent.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;

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
}
