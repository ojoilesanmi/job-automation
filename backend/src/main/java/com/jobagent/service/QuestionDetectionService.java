package com.jobagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobagent.dto.DetectedQuestion;
import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.Job;
import com.jobagent.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionDetectionService {

    private final JobRepository jobRepository;
    private final AiServiceClient aiServiceClient;

    @Transactional(readOnly = true)
    public List<DetectedQuestion> detectQuestions(UUID userId, UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        String description = job.getDescription();
        if (description == null || description.isBlank()) {
            return List.of();
        }

        try {
            JsonNode parsed = aiServiceClient.parseJob(job.getTitle(), job.getCompany(), description);
            if (parsed != null && parsed.has("questions")) {
                return extractQuestionsFromAi(parsed.get("questions"));
            }
        } catch (Exception e) {
            log.warn("AI question detection failed for job {}: {}", jobId, e.getMessage());
        }

        return extractQuestionsFromDescription(description);
    }

    private List<DetectedQuestion> extractQuestionsFromAi(JsonNode questionsNode) {
        List<DetectedQuestion> questions = new ArrayList<>();
        if (!questionsNode.isArray()) return questions;

        for (JsonNode node : questionsNode) {
            List<Map<String, String>> options = new ArrayList<>();
            if (node.has("options") && node.get("options").isArray()) {
                for (JsonNode opt : node.get("options")) {
                    options.add(Map.of(
                            "value", opt.path("value").asText(""),
                            "label", opt.path("label").asText("")
                    ));
                }
            }
            questions.add(new DetectedQuestion(
                    node.path("fieldName").asText(""),
                    node.path("fieldType").asText("text"),
                    node.path("label").asText(""),
                    node.path("required").asBoolean(false),
                    options
            ));
        }
        return questions;
    }

    private List<DetectedQuestion> extractQuestionsFromDescription(String description) {
        List<DetectedQuestion> questions = new ArrayList<>();
        String lower = description.toLowerCase();

        List<String> questionPatterns = Arrays.asList(
                "why are you interested",
                "cover letter",
                "salary expectation",
                "available to start",
                "notice period",
                "work authorization",
                "visa sponsorship",
                "relocate",
                "references",
                "portfolio",
                "github",
                "website",
                "linkedin",
                "additional information",
                "how did you hear",
                "tell us about yourself",
                "years of experience"
        );

        for (String pattern : questionPatterns) {
            if (lower.contains(pattern)) {
                String fieldType = "text";
                if (pattern.contains("salary") || pattern.contains("years")) {
                    fieldType = "number";
                }
                questions.add(new DetectedQuestion(
                        pattern.replaceAll("\\s+", "_"),
                        fieldType,
                        capitalizeFirst(pattern),
                        false,
                        List.of()
                ));
            }
        }

        return questions;
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1) + "?";
    }
}
