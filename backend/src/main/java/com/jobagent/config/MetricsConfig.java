package com.jobagent.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    private final MeterRegistry meterRegistry;

    public MetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Bean
    public Counter jobImportCounter() {
        return Counter.builder("jobagent.jobs.imported")
                .description("Number of jobs imported")
                .tag("source", "manual")
                .register(meterRegistry);
    }

    @Bean
    public Counter coverLetterGeneratedCounter() {
        return Counter.builder("jobagent.cover_letters.generated")
                .description("Number of cover letters generated")
                .register(meterRegistry);
    }

    @Bean
    public Counter applicationSubmittedCounter() {
        return Counter.builder("jobagent.applications.submitted")
                .description("Number of applications submitted")
                .register(meterRegistry);
    }

    @Bean
    public Counter matchScoredCounter() {
        return Counter.builder("jobagent.matches.scored")
                .description("Number of jobs scored")
                .register(meterRegistry);
    }

    @Bean
    public Counter authLoginCounter() {
        return Counter.builder("jobagent.auth.logins")
                .description("Number of login attempts")
                .register(meterRegistry);
    }

    @Bean
    public Counter authFailedLoginCounter() {
        return Counter.builder("jobagent.auth.login_failures")
                .description("Number of failed login attempts")
                .register(meterRegistry);
    }

    @Bean
    public Timer coverLetterGenerationTimer() {
        return Timer.builder("jobagent.cover_letters.generation_time")
                .description("Time taken to generate cover letters")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    @Bean
    public Timer matchScoringTimer() {
        return Timer.builder("jobagent.matches.scoring_time")
                .description("Time taken to score jobs")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }
}
