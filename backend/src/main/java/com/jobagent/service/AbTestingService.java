package com.jobagent.service;

import com.jobagent.model.AbTestResult;
import com.jobagent.repository.AbTestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AbTestingService {

    private final AbTestResultRepository abTestResultRepository;

    @Transactional(readOnly = true)
    public String getVariant(UUID userId, String experimentName) {
        int hash = hashString(userId.toString() + experimentName);
        int variant = (hash & 0x7fffffff) % 2;
        return variant == 0 ? "control" : "variant";
    }

    @Transactional
    public void recordResult(UUID userId, String experimentName, String variant, String outcome) {
        AbTestResult result = AbTestResult.builder()
                .userId(userId)
                .experimentName(experimentName)
                .variant(variant)
                .outcome(outcome)
                .build();
        abTestResultRepository.save(result);
        log.info("A/B test result recorded: user={}, experiment={}, variant={}, outcome={}",
                userId, experimentName, variant, outcome);
    }

    private int hashString(String input) {
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        int hash = 0;
        for (byte b : bytes) {
            hash = 31 * hash + b;
        }
        return hash;
    }
}
