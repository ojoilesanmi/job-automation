package com.jobagent.worker;

import com.jobagent.model.Job;
import com.jobagent.model.JobSource;
import com.jobagent.repository.JobRepository;
import com.jobagent.repository.JobSourceRepository;
import com.jobagent.service.DuplicateJobDetectionService;
import com.jobagent.service.QueueProducerService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobDiscoveryService {

    private final JobSourceRepository jobSourceRepository;
    private final JobRepository jobRepository;
    private final List<JobSourceConnector> connectors;
    private final MeterRegistry meterRegistry;
    private final JobSourceHealthMonitor healthMonitor;
    private final QueueProducerService queueProducerService;
    private final DuplicateJobDetectionService duplicateJobDetectionService;

    private static final int MAX_JOBS_PER_SOURCE = 50;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    @Scheduled(fixedDelayString = "${app.discovery.interval:3600000}", initialDelay = 60000)
    public void runDiscovery() {
        log.info("Starting job discovery cycle");
        Timer.Sample sample = Timer.start(meterRegistry);

        Map<String, JobSourceConnector> connectorMap = connectors.stream()
                .collect(Collectors.toMap(JobSourceConnector::getSourceType, Function.identity()));

        List<JobSource> activeSources = jobSourceRepository.findAll().stream()
                .filter(JobSource::getEnabled)
                .toList();

        int totalNew = 0;
        for (JobSource source : activeSources) {
            JobSourceConnector connector = connectorMap.get(source.getSourceType());
            if (connector == null) {
                log.warn("No connector for source type: {}", source.getSourceType());
                continue;
            }

            try {
                List<Job> fetched = RetryableJobFetch.executeWithRetry(
                        () -> connector.fetchJobs(source, MAX_JOBS_PER_SOURCE),
                        MAX_RETRIES, RETRY_DELAY_MS,
                        "fetchJobs:" + source.getSourceType());
                int saved = saveNewJobs(fetched);
                totalNew += saved;
                if (saved > 0) {
                    for (Job job : fetched.stream().limit(saved).toList()) {
                        queueProducerService.sendJobMatching(job.getId());
                    }
                }
                healthMonitor.recordSuccess(source.getSourceType());
                log.info("Source '{}': fetched {}, saved {} new", source.getName(), fetched.size(), saved);
            } catch (Exception e) {
                healthMonitor.recordFailure(source.getSourceType());
                log.error("Discovery failed for source '{}': {}", source.getName(), e.getMessage());
            }
        }

        sample.stop(Timer.builder("jobagent.discovery.cycle_time")
                .description("Time for full discovery cycle")
                .register(meterRegistry));
        Counter.builder("jobagent.discovery.jobs_found")
                .description("Total new jobs discovered")
                .register(meterRegistry)
                .increment(totalNew);

        log.info("Discovery cycle complete: {} new jobs from {} sources", totalNew, activeSources.size());
    }

    private int saveNewJobs(List<Job> jobs) {
        int saved = 0;
        for (Job job : jobs) {
            try {
                boolean duplicate = RetryableJobFetch.executeWithRetry(
                        () -> duplicateJobDetectionService.findDuplicate(job).isPresent(),
                        2, RETRY_DELAY_MS,
                        "duplicateCheck:" + job.getExternalJobId());
                if (!duplicate) {
                    RetryableJobFetch.executeWithRetry(
                            () -> jobRepository.save(job),
                            2, RETRY_DELAY_MS,
                            "saveJob:" + job.getExternalJobId());
                    saved++;
                } else {
                    log.debug("Skipped duplicate job: {} at {}", job.getTitle(), job.getCompany());
                }
            } catch (Exception e) {
                log.debug("Skip duplicate job: {}", job.getExternalJobId());
            }
        }
        return saved;
    }

    public List<Job> triggerManualDiscovery(UUID sourceId) {
        JobSource source = jobSourceRepository.findById(sourceId)
                .orElseThrow(() -> new com.jobagent.exception.ResourceNotFoundException("Job source not found"));

        JobSourceConnector connector = connectors.stream()
                .filter(c -> c.getSourceType().equals(source.getSourceType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No connector for type: " + source.getSourceType()));

        List<Job> fetched = connector.fetchJobs(source, MAX_JOBS_PER_SOURCE);
        saveNewJobs(fetched);
        return fetched;
    }

    public Map<String, String> getAvailableConnectors() {
        return connectors.stream()
                .collect(Collectors.toMap(
                        JobSourceConnector::getSourceType,
                        c -> c.getClass().getSimpleName()));
    }
}
