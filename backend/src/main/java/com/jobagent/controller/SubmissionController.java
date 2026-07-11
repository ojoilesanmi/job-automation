package com.jobagent.controller;

import com.jobagent.dto.*;
import com.jobagent.model.SubmissionLog;
import com.jobagent.security.RequirePermission;
import com.jobagent.security.SecurityUtils;
import com.jobagent.service.ApplicationSubmissionService;
import com.jobagent.service.PlaywrightSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final ApplicationSubmissionService submissionService;
    private final PlaywrightSubmissionService playwrightService;

    @GetMapping("/checklist/{applicationId}")
    @RequirePermission("applications:read")
    public ResponseEntity<BaseResponse<SubmissionChecklistResponse>> getChecklist(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(BaseResponse.success(
                submissionService.getSubmissionChecklist(SecurityUtils.getCurrentUserId(), applicationId)));
    }

    @GetMapping("/logs/{applicationId}")
    @RequirePermission("applications:read")
    public ResponseEntity<BaseResponse<List<SubmissionLog>>> getLogs(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(BaseResponse.success(
                submissionService.getSubmissionLogs(SecurityUtils.getCurrentUserId(), applicationId)));
    }

    @PostMapping("/submit")
    @RequirePermission("applications:write")
    public ResponseEntity<BaseResponse<Map<String, Object>>> submit(@RequestBody SubmitApplicationRequest request) {
        return ResponseEntity.ok(BaseResponse.success("Submission initiated",
                playwrightService.submitApplication(SecurityUtils.getCurrentUserId(), request)));
    }
}
