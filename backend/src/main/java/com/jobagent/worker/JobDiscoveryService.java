package com.jobagent.worker;

import com.jobagent.model.Job;
import com.jobagent.model.JobSource;
import com.jobagent.repository.JobRepository;
import com.jobagent.repository.JobSourceRepository;
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

    private static final int MAX_JOBS_PER_SOURCE = 50;

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
                List<Job> fetched = connector.fetchJobs(source, MAX_JOBS_PER_SOURCE);
                int saved = saveNewJobs(fetched);
                totalNew += saved;
                log.info("Source '{}': fetched {}, saved {} new", source.getName(), fetched.size(), saved);
            } catch (Exception e) {
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
                boolean exists = jobRepository.existsBySourceIdAndExternalJobId(
                        job.getSource().getId(), job.getExternalJobId());
                if (!exists) {
                    jobRepository.save(job);
                    saved++;
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
