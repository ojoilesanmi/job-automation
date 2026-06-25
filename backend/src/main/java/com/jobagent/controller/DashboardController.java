package com.jobagent.controller;

import com.jobagent.dto.*;
import com.jobagent.security.SecurityUtils;
import com.jobagent.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public ResponseEntity<BaseResponse<DashboardOverviewResponse>> getOverview() {
        return ResponseEntity.ok(BaseResponse.success(dashboardService.getOverview(SecurityUtils.getCurrentUserId())));
    }

    @GetMapping("/stats")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getStats() {
        DashboardOverviewResponse overview = dashboardService.getOverview(SecurityUtils.getCurrentUserId());
        Map<String, Object> stats = Map.of(
            "totalJobs", overview.totalJobsDiscovered(),
            "pendingApprovals", overview.pendingApproval(),
            "appliedThisWeek", overview.applicationsSubmitted(),
            "averageMatchScore", overview.responseRate()
        );
        return ResponseEntity.ok(BaseResponse.success(stats));
    }

    @GetMapping("/pipeline")
    public ResponseEntity<BaseResponse<PipelineReportResponse>> getPipeline() {
        return ResponseEntity.ok(BaseResponse.success(
                dashboardService.getPipelineReport(SecurityUtils.getCurrentUserId())));
    }
}
