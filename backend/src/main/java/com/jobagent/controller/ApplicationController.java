package com.jobagent.controller;

import com.jobagent.dto.*;
import com.jobagent.security.RequirePermission;
import com.jobagent.security.SecurityUtils;
import com.jobagent.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @RequirePermission("application:write")
    public ResponseEntity<BaseResponse<ApplicationResponse>> createApplication(
            @Valid @RequestBody CreateApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("Application created",
                        applicationService.createApplication(SecurityUtils.getCurrentUserId(), request)));
    }

    @GetMapping
    @RequirePermission("application:read")
    public ResponseEntity<BaseResponse<ApplicationListResponse>> getApplications(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(BaseResponse.success(
                applicationService.getApplications(SecurityUtils.getCurrentUserId(), status, page, size)));
    }

    @GetMapping("/{id}")
    @RequirePermission("application:read")
    public ResponseEntity<BaseResponse<ApplicationResponse>> getApplication(@PathVariable UUID id) {
        return ResponseEntity.ok(BaseResponse.success(
                applicationService.getApplication(SecurityUtils.getCurrentUserId(), id)));
    }

    @PutMapping("/{id}/status")
    @RequirePermission("application:write")
    public ResponseEntity<BaseResponse<ApplicationResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(BaseResponse.success("Status updated",
                applicationService.updateStatus(SecurityUtils.getCurrentUserId(), id, body.get("status"))));
    }

    @PostMapping("/{id}/submit")
    @RequirePermission("application:submit")
    public ResponseEntity<BaseResponse<ApplicationResponse>> submitApplication(@PathVariable UUID id) {
        return ResponseEntity.ok(BaseResponse.success("Application submitted",
                applicationService.submitApplication(SecurityUtils.getCurrentUserId(), id)));
    }

    @PostMapping("/{id}/notes")
    @RequirePermission("application:write")
    public ResponseEntity<BaseResponse<ApplicationResponse>> addNote(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(BaseResponse.success("Note added",
                applicationService.addNote(SecurityUtils.getCurrentUserId(), id, body.get("notes"))));
    }
}
