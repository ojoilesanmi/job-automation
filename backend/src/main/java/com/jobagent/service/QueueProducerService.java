package com.jobagent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueProducerService {

    private final RabbitTemplate rabbitTemplate;

    public void sendJobDiscovery(UUID sourceId, String sourceType) {
        try {
            rabbitTemplate.convertAndSend("job.discovery", Map.of(
                    "sourceId", sourceId.toString(),
                    "sourceType", sourceType
            ));
            log.info("Sent job discovery message for source: {}", sourceType);
        } catch (Exception e) {
            log.warn("Failed to send to job.discovery queue (RabbitMQ may be down): {}", e.getMessage());
        }
    }

    public void sendJobMatching(UUID jobId) {
        try {
            rabbitTemplate.convertAndSend("job.matching", Map.of(
                    "jobId", jobId.toString()
            ));
            log.info("Sent job matching message for job: {}", jobId);
        } catch (Exception e) {
            log.warn("Failed to send to job.matching queue: {}", e.getMessage());
        }
    }

    public void sendCoverLetterGeneration(UUID jobId, UUID userId, String tone) {
        try {
            rabbitTemplate.convertAndSend("cover.letter.generation", Map.of(
                    "jobId", jobId.toString(),
                    "userId", userId.toString(),
                    "tone", tone != null ? tone : "professional"
            ));
            log.info("Sent cover letter generation message for job: {}", jobId);
        } catch (Exception e) {
            log.warn("Failed to send to cover.letter.generation queue: {}", e.getMessage());
        }
    }

    public void sendApplicationSubmission(UUID applicationId) {
        try {
            rabbitTemplate.convertAndSend("application.submission", Map.of(
                    "applicationId", applicationId.toString()
            ));
            log.info("Sent application submission message for application: {}", applicationId);
        } catch (Exception e) {
            log.warn("Failed to send to application.submission queue: {}", e.getMessage());
        }
    }
}
