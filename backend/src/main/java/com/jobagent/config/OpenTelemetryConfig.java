package com.jobagent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "management.tracing.enabled", havingValue = "true", matchIfMissing = true)
public class OpenTelemetryConfig {

    public OpenTelemetryConfig() {
        log.info("OpenTelemetry tracing enabled");
    }
}
