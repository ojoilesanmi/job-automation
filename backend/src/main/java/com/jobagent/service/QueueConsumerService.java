package com.jobagent.service;

import com.jobagent.dto.GenerateCoverLetterRequest;
import com.jobagent.dto.SubmitApplicationRequest;
import com.jobagent.worker.JobDiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueConsumerService {

    private final JobDiscoveryService jobDiscoveryService;
    private final MatchingEngine matchingEngine;
    private final CoverLetterService coverLetterService;
    private final PlaywrightSubmissionService submissionService;

    @RabbitListener(queues = "job.discovery")
    public void handleJobDiscovery(Map<String, String> message) {
        try {
            UUID sourceId = UUID.fromString(message.get("sourceId"));
            log.info("Processing job discovery for source: {}", message.get("sourceType"));
            jobDiscoveryService.triggerManualDiscovery(sourceId);
        } catch (Exception e) {
            log.error("Failed to process job discovery message: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "job.matching")
    public void handleJobMatching(Map<String, String> message) {
        try {
            UUID jobId = UUID.fromString(message.get("jobId"));
            log.info("Processing job matching for job: {}", jobId);
            matchingEngine.scoreJob(null, jobId);
        } catch (Exception e) {
            log.error("Failed to process job matching message: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "cover.letter.generation")
    public void handleCoverLetterGeneration(Map<String, String> message) {
        try {
            UUID jobId = UUID.fromString(message.get("jobId"));
            UUID userId = UUID.fromString(message.get("userId"));
            String tone = message.get("tone");
            log.info("Processing cover letter generation for job: {}", jobId);
            coverLetterService.generateCoverLetter(userId,
                    new GenerateCoverLetterRequest(jobId, null, tone));
        } catch (Exception e) {
            log.error("Failed to process cover letter generation message: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "application.submission")
    public void handleApplicationSubmission(Map<String, String> message) {
        try {
            UUID applicationId = UUID.fromString(message.get("applicationId"));
            log.info("Processing application submission: {}", applicationId);
            submissionService.submitApplication(null,
                    new SubmitApplicationRequest(applicationId, "browser", null));
        } catch (Exception e) {
            log.error("Failed to process application submission message: {}", e.getMessage());
        }
    }
}
