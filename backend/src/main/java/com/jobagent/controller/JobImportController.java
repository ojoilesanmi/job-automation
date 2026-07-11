package com.jobagent.controller;

import com.jobagent.dto.BaseResponse;
import com.jobagent.dto.ImportJobRequest;
import com.jobagent.model.Job;
import com.jobagent.security.RequirePermission;
import com.jobagent.service.JobImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobImportController {

    private final JobImportService jobImportService;

    @PostMapping("/import-url")
    @RequirePermission("jobs:write")
    public ResponseEntity<BaseResponse<Job>> importJob(@RequestBody ImportJobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("Job imported", jobImportService.importFromUrl(request)));
    }
}
