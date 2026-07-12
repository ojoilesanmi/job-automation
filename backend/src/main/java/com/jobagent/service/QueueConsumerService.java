package com.jobagent.service;

import com.jobagent.dto.GenerateCoverLetterRequest;
import com.jobagent.dto.SubmitApplicationRequest;
import com.jobagent.worker.JobDiscoveryService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
    private final MeterRegistry meterRegistry;

    @RabbitListener(queues = "job.discovery")
    public void handleJobDiscovery(Map<String, String> message) {
        try {
            UUID sourceId = UUID.fromString(message.get("sourceId"));
            log.info("Processing job discovery for source: {}", message.get("sourceType"));
            jobDiscoveryService.triggerManualDiscovery(sourceId);
            Counter.builder("jobagent.queue.messages_processed")
                    .description("Number of queue messages processed")
                    .tag("queue", "job.discovery")
                    .register(meterRegistry)
                    .increment();
        } catch (Exception e) {
            log.error("Failed to process job discovery message: {}", e.getMessage());
            Counter.builder("jobagent.queue.messages_failed")
                    .description("Number of queue messages that failed processing")
                    .tag("queue", "job.discovery")
                    .register(meterRegistry)
                    .increment();
        }
    }

    @RabbitListener(queues = "job.matching")
    public void handleJobMatching(Map<String, String> message) {
        try {
            UUID jobId = UUID.fromString(message.get("jobId"));
            log.info("Processing job matching for job: {}", jobId);
            matchingEngine.scoreJob(null, jobId);
            Counter.builder("jobagent.queue.messages_processed")
                    .description("Number of queue messages processed")
                    .tag("queue", "job.matching")
                    .register(meterRegistry)
                    .increment();
        } catch (Exception e) {
            log.error("Failed to process job matching message: {}", e.getMessage());
            Counter.builder("jobagent.queue.messages_failed")
                    .description("Number of queue messages that failed processing")
                    .tag("queue", "job.matching")
                    .register(meterRegistry)
                    .increment();
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
            Counter.builder("jobagent.queue.messages_processed")
                    .description("Number of queue messages processed")
                    .tag("queue", "cover.letter.generation")
                    .register(meterRegistry)
                    .increment();
        } catch (Exception e) {
            log.error("Failed to process cover letter generation message: {}", e.getMessage());
            Counter.builder("jobagent.queue.messages_failed")
                    .description("Number of queue messages that failed processing")
                    .tag("queue", "cover.letter.generation")
                    .register(meterRegistry)
                    .increment();
        }
    }

    @RabbitListener(queues = "application.submission")
    public void handleApplicationSubmission(Map<String, String> message) {
        try {
            UUID applicationId = UUID.fromString(message.get("applicationId"));
            log.info("Processing application submission: {}", applicationId);
            submissionService.submitApplication(null,
                    new SubmitApplicationRequest(applicationId, "browser", null));
            Counter.builder("jobagent.queue.messages_processed")
                    .description("Number of queue messages processed")
                    .tag("queue", "application.submission")
                    .register(meterRegistry)
                    .increment();
        } catch (Exception e) {
            log.error("Failed to process application submission message: {}", e.getMessage());
            Counter.builder("jobagent.queue.messages_failed")
                    .description("Number of queue messages that failed processing")
                    .tag("queue", "application.submission")
                    .register(meterRegistry)
                    .increment();
        }
    }
}
