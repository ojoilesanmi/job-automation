package com.jobagent.service;

import com.jobagent.model.*;
import com.jobagent.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningLoopService {

    private final UserFeedbackRepository feedbackRepository;
    private final JobMatchRepository matchRepository;
    private final UserPreferencesRepository preferencesRepository;

    @Transactional
    public void recordFeedback(UUID userId, UUID matchId, String feedbackType, String reason) {
        JobMatch match = matchRepository.findById(matchId).orElse(null);
        User user = new User();
        user.setId(userId);

        UserFeedback feedback = UserFeedback.builder()
                .user(user)
                .jobMatch(match)
                .feedbackType(feedbackType)
                .reason(reason)
                .originalScore(match != null ? match.getFitScore().doubleValue() : null)
                .originalStatus(match != null ? match.getStatus() : null)
                .build();
        feedbackRepository.save(feedback);
        log.info("Feedback recorded: user={}, match={}, type={}", userId, matchId, feedbackType);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLearningInsights(UUID userId) {
        List<Object[]> feedbackCounts = feedbackRepository.countByFeedbackTypeGrouped(userId);
        Map<String, Long> byType = new LinkedHashMap<>();
        for (Object[] row : feedbackCounts) {
            byType.put((String) row[0], (Long) row[1]);
        }

        long total = byType.values().stream().mapToLong(Long::longValue).sum();
        long approved = byType.getOrDefault("approved", 0L);
        long rejected = byType.getOrDefault("rejected", 0L);
        double approvalRate = total > 0 ? (double) approved / total * 100 : 0;

        return Map.of(
                "feedbackCounts", byType,
                "totalDecisions", total,
                "approvalRate", Math.round(approvalRate * 100.0) / 100.0
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getCompanyFeedbackCounts(UUID userId, String company) {
        List<Object[]> rows = feedbackRepository.countByFeedbackTypeAndCompany(userId, company);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            counts.put((String) row[0], (Long) row[1]);
        }
        return counts;
    }
}
