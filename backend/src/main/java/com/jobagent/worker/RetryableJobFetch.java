package com.jobagent.worker;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
public class RetryableJobFetch {

    public static <T> T executeWithRetry(Supplier<T> operation, int maxRetries, long delayMs, String context) {
        int attempt = 0;
        while (attempt <= maxRetries) {
            try {
                return operation.get();
            } catch (Exception e) {
                attempt++;
                if (attempt > maxRetries) {
                    log.error("All {} retries exhausted for {}: {}", maxRetries, context, e.getMessage());
                    throw new RuntimeException("Failed after " + maxRetries + " retries: " + context, e);
                }
                log.warn("Attempt {}/{} failed for {}: {}. Retrying in {}ms...",
                        attempt, maxRetries, context, e.getMessage(), delayMs);
                try {
                    Thread.sleep(delayMs * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
        throw new RuntimeException("Unexpected retry failure");
    }
}
