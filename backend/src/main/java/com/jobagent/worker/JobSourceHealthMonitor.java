package com.jobagent.worker;

import com.jobagent.model.JobSource;
import com.jobagent.repository.JobSourceRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobSourceHealthMonitor {

    private final JobSourceRepository jobSourceRepository;
    private final MeterRegistry meterRegistry;

    private final Map<String, Boolean> sourceHealth = new ConcurrentHashMap<>();
    private final Map<String, OffsetDateTime> lastSuccessTime = new ConcurrentHashMap<>();
    private final Map<String, Integer> consecutiveFailures = new ConcurrentHashMap<>();

    public void recordSuccess(String sourceType) {
        sourceHealth.put(sourceType, true);
        lastSuccessTime.put(sourceType, OffsetDateTime.now());
        consecutiveFailures.put(sourceType, 0);
    }

    public void recordFailure(String sourceType) {
        sourceHealth.put(sourceType, false);
        consecutiveFailures.merge(sourceType, 1, Integer::sum);
        int failures = consecutiveFailures.get(sourceType);
        if (failures >= 3) {
            log.error("Source {} has failed {} consecutive times", sourceType, failures);
        }
    }

    public Map<String, Boolean> getSourceHealth() {
        return Map.copyOf(sourceHealth);
    }

    @Scheduled(fixedDelay = 300000)
    public void registerMetrics() {
        for (String source : sourceHealth.keySet()) {
            Gauge.builder("jobagent.source.health", () -> sourceHealth.getOrDefault(source, true) ? 1 : 0)
                    .tag("source", source)
                    .description("Source health status")
                    .register(meterRegistry);
            Gauge.builder("jobagent.source.consecutive_failures", () -> consecutiveFailures.getOrDefault(source, 0))
                    .tag("source", source)
                    .description("Consecutive failure count")
                    .register(meterRegistry);
        }
    }
}
