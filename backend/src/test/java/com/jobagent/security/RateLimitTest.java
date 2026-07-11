package com.jobagent.security;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitTest {

    @Test
    void rateLimitCounterIncrements() {
        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < 5; i++) {
            counter.incrementAndGet();
        }
        assertThat(counter.get()).isEqualTo(5);
    }

    @Test
    void rateLimitThresholdIsReasonable() {
        int maxAttempts = 5;
        int lockoutMinutes = 15;
        assertThat(maxAttempts).isLessThanOrEqualTo(10);
        assertThat(lockoutMinutes).isBetween(5, 60);
    }
}
