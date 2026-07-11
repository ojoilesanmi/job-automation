package com.jobagent.service;

import com.jobagent.dto.SubmitApplicationRequest;
import com.jobagent.exception.ForbiddenException;
import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.*;
import com.jobagent.repository.*;
import com.jobagent.worker.RetryableJobFetch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaywrightSubmissionService {

    private final ApplicationRepository applicationRepository;
    private final SubmissionLogRepository submissionLogRepository;

    @Value("${app.playwright.script-path:../playwright}")
    private String playwrightScriptPath;

    @Transactional
    public Map<String, Object> submitApplication(UUID userId, SubmitApplicationRequest request) {
        Application app = applicationRepository.findById(request.applicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (userId != null && !app.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }

        String url = app.getJob().getApplicationUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("No application URL available");
        }

        SubmissionLog logEntry = SubmissionLog.builder()
                .application(app)
                .user(app.getUser())
                .method(request.method() != null ? request.method() : "browser")
                .status("pending")
                .requestPayload(url)
                .build();

        try {
            Map<String, Object> result = RetryableJobFetch.executeWithRetry(() -> {
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                            "node", playwrightScriptPath + "/submit.js",
                            "--url", url,
                            "--method", request.method() != null ? request.method() : "browser"
                    );
                    pb.directory(new File(playwrightScriptPath));
                    pb.redirectErrorStream(true);

                    Process process = pb.start();
                    String output = new String(process.getInputStream().readAllBytes());
                    int exitCode = process.waitFor();

                    if (exitCode != 0) {
                        throw new RuntimeException("Playwright exited with code " + exitCode + ": " + output);
                    }
                    return Map.<String, Object>of("output", output, "exitCode", exitCode);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, 3, 2000, "playwright-submission-" + request.applicationId());

            logEntry.setResponsePayload((String) result.get("output"));
            logEntry.setStatus("success");
            submissionLogRepository.save(logEntry);

            return Map.of(
                    "status", "success",
                    "output", result.get("output"),
                    "applicationId", app.getId().toString()
            );
        } catch (Exception e) {
            log.error("Playwright submission failed after retries: {}", e.getMessage());
            logEntry.setStatus("failed");
            logEntry.setErrorMessage(e.getMessage());
            submissionLogRepository.save(logEntry);
            throw new RuntimeException("Submission failed after retries: " + e.getMessage(), e);
        }
    }
}
