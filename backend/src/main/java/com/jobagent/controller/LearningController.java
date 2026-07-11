package com.jobagent.controller;

import com.jobagent.dto.BaseResponse;
import com.jobagent.security.RequirePermission;
import com.jobagent.security.SecurityUtils;
import com.jobagent.service.AutoApplyService;
import com.jobagent.service.LearningLoopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
public class LearningController {

    private final LearningLoopService learningLoopService;
    private final AutoApplyService autoApplyService;

    @PostMapping("/feedback/{matchId}")
    @RequirePermission("matches:write")
    public ResponseEntity<BaseResponse<Void>> recordFeedback(
            @PathVariable UUID matchId,
            @RequestBody Map<String, String> body) {
        learningLoopService.recordFeedback(
                SecurityUtils.getCurrentUserId(), matchId,
                body.get("feedbackType"), body.get("reason"));
        return ResponseEntity.ok(BaseResponse.success("Feedback recorded", null));
    }

    @GetMapping("/insights")
    @RequirePermission("matches:read")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getInsights() {
        return ResponseEntity.ok(BaseResponse.success(
                learningLoopService.getLearningInsights(SecurityUtils.getCurrentUserId())));
    }

    @PostMapping("/auto-apply")
    @RequirePermission("applications:write")
    public ResponseEntity<BaseResponse<Map<String, Object>>> triggerAutoApply() {
        return ResponseEntity.ok(BaseResponse.success("Auto-apply processed",
                autoApplyService.processAutoApplyForUser(SecurityUtils.getCurrentUserId())));
    }
}
