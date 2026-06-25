package com.jobagent.controller;

import com.jobagent.dto.*;
import com.jobagent.security.RequirePermission;
import com.jobagent.security.SecurityUtils;
import com.jobagent.service.FollowUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/follow-ups")
@RequiredArgsConstructor
public class FollowUpController {

    private final FollowUpService followUpService;

    @GetMapping
    @RequirePermission("applications:read")
    public ResponseEntity<BaseResponse<List<FollowUpResponse>>> getUpcoming() {
        return ResponseEntity.ok(BaseResponse.success(
                followUpService.getUpcomingFollowUps(SecurityUtils.getCurrentUserId())));
    }

    @PostMapping
    @RequirePermission("applications:write")
    public ResponseEntity<BaseResponse<FollowUpResponse>> scheduleFollowUp(
            @RequestBody Map<String, String> body) {
        UUID applicationId = UUID.fromString(body.get("applicationId"));
        OffsetDateTime nextFollowUpAt = OffsetDateTime.parse(body.get("nextFollowUpAt"));
        return ResponseEntity.ok(BaseResponse.success(
                followUpService.scheduleFollowUp(SecurityUtils.getCurrentUserId(), applicationId, nextFollowUpAt)));
    }
}
