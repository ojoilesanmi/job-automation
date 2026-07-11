package com.jobagent.service;

import com.jobagent.model.Application;
import com.jobagent.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailClassifierService {

    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    private static final Map<String, List<Pattern>> CLASSIFICATION_PATTERNS = Map.of(
            "interview", List.of(
                    Pattern.compile("(?i)interview\\s+(invite|invitation|scheduled|confirmation)"),
                    Pattern.compile("(?i)schedule.*interview"),
                    Pattern.compile("(?i)we.*like.*to.*invite.*interview"),
                    Pattern.compile("(?i)proceed.*next.*round")
            ),
            "rejection", List.of(
                    Pattern.compile("(?i)unfortunately.*not.*proceed"),
                    Pattern.compile("(?i)position.*filled|role.*filled"),
                    Pattern.compile("(?i)not.*selected|decided.*move.*forward.*another"),
                    Pattern.compile("(?i)regret.*inform")
            ),
            "offer", List.of(
                    Pattern.compile("(?i)offer.*letter|job.*offer"),
                    Pattern.compile("(?i)pleased.*offer"),
                    Pattern.compile("(?i)compensation.*package"),
                    Pattern.compile("(?i)start.*date.*position")
            ),
            "assessment", List.of(
                    Pattern.compile("(?i)assessment|technical.*test|coding.*challenge"),
                    Pattern.compile("(?i)complete.*following.*exercise"),
                    Pattern.compile("(?i)take.*home.*assignment")
            )
    );

    @Transactional
    public Map<String, Object> classifyAndProcess(UUID userId, List<Map<String, String>> emails) {
        List<Map<String, String>> classified = new ArrayList<>();
        int updated = 0;

        for (Map<String, String> email : emails) {
            String subject = email.getOrDefault("subject", "");
            String snippet = email.getOrDefault("snippet", "");
            String combined = subject + " " + snippet;

            String classification = classify(combined);
            if (!"unknown".equals(classification)) {
                email.put("classification", classification);
                classified.add(email);

                updated += autoUpdateStatus(userId, email, classification);
            }
        }

        if (!classified.isEmpty()) {
            notificationService.createNotification(userId, "email_classified",
                    "Emails classified",
                    classified.size() + " emails classified, " + updated + " applications updated",
                    null, "email");
        }

        return Map.of("classified", classified.size(), "applicationsUpdated", updated);
    }

    private String classify(String text) {
        for (Map.Entry<String, List<Pattern>> entry : CLASSIFICATION_PATTERNS.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(text).find()) {
                    return entry.getKey();
                }
            }
        }
        return "unknown";
    }

    private int autoUpdateStatus(UUID userId, Map<String, String> email, String classification) {
        String from = email.getOrDefault("from", "").toLowerCase();
        String subject = email.getOrDefault("subject", "").toLowerCase();

        Optional<Application> appOpt = applicationRepository.findByUserIdOrderByCreatedAtDesc(userId,
                PageRequest.of(0, 100))
                .getContent().stream()
                .filter(a -> {
                    String company = a.getJob().getCompany().toLowerCase();
                    return from.contains(company) || subject.contains(a.getJob().getTitle().toLowerCase());
                })
                .findFirst();

        if (appOpt.isEmpty()) return 0;

        Application app = appOpt.get();
        String newStatus = mapClassificationToStatus(classification);
        if (newStatus != null && !newStatus.equals(app.getStatus())) {
            String oldStatus = app.getStatus();
            app.setStatus(newStatus);
            applicationRepository.save(app);
            log.info("Auto-updated application {} from {} to {} based on email", app.getId(), oldStatus, newStatus);
            return 1;
        }
        return 0;
    }

    private String mapClassificationToStatus(String classification) {
        return switch (classification) {
            case "interview" -> "interview";
            case "rejection" -> "rejected";
            case "offer" -> "offer";
            case "assessment" -> "assessment";
            default -> null;
        };
    }
}
