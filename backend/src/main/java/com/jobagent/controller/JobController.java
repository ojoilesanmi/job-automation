package com.jobagent.controller;

import com.jobagent.dto.*;
import com.jobagent.security.RequirePermission;
import com.jobagent.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping
    @RequirePermission("job:read")
    public ResponseEntity<BaseResponse<JobListResponse>> searchJobs(
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String remoteType,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Boolean relocation,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) String seniority,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime datePostedAfter,
            @RequestParam(required = false) BigDecimal fitScoreMin,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(BaseResponse.success(
                jobService.searchJobs(company, country, remoteType, source, relocation, salaryMin,
                        seniority, role, datePostedAfter, fitScoreMin, search, page, size)));
    }

    @GetMapping("/{id}")
    @RequirePermission("job:read")
    public ResponseEntity<BaseResponse<JobResponse>> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(BaseResponse.success(jobService.getJob(id)));
    }

    @PostMapping("/import-url")
    @RequirePermission("job:write")
    public ResponseEntity<BaseResponse<JobResponse>> importJob(@Valid @RequestBody ImportJobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("Job imported successfully", jobService.importJob(request)));
    }
}
