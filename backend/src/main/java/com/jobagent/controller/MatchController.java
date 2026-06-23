package com.jobagent.controller;

import com.jobagent.dto.*;
import com.jobagent.security.RequirePermission;
import com.jobagent.security.SecurityUtils;
import com.jobagent.service.MatchingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchingEngine matchingEngine;

    @GetMapping
    @RequirePermission("match:read")
    public ResponseEntity<BaseResponse<MatchListResponse>> getMatches(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(BaseResponse.success(
                matchingEngine.getMatches(SecurityUtils.getCurrentUserId(), status, page, size)));
    }

    @GetMapping("/{id}")
    @RequirePermission("match:read")
    public ResponseEntity<BaseResponse<JobMatchResponse>> getMatch(@PathVariable UUID id) {
        return ResponseEntity.ok(BaseResponse.success(matchingEngine.getMatch(id)));
    }

    @PostMapping("/{id}/approve")
    @RequirePermission("match:write")
    public ResponseEntity<BaseResponse<Void>> approveMatch(@PathVariable UUID id) {
        matchingEngine.updateMatchStatus(id, "approved");
        return ResponseEntity.ok(BaseResponse.success("Match approved", null));
    }

    @PostMapping("/{id}/reject")
    @RequirePermission("match:write")
    public ResponseEntity<BaseResponse<Void>> rejectMatch(@PathVariable UUID id) {
        matchingEngine.updateMatchStatus(id, "rejected");
        return ResponseEntity.ok(BaseResponse.success("Match rejected", null));
    }

    @PostMapping("/{id}/save")
    @RequirePermission("match:write")
    public ResponseEntity<BaseResponse<Void>> saveMatch(@PathVariable UUID id) {
        matchingEngine.updateMatchStatus(id, "saved");
        return ResponseEntity.ok(BaseResponse.success("Match saved", null));
    }
}
