package com.jobagent.controller;

import com.jobagent.dto.BaseResponse;
import com.jobagent.security.OAuthStateToken;
import com.jobagent.security.RequirePermission;
import com.jobagent.security.SecurityUtils;
import com.jobagent.service.EmailClassifierService;
import com.jobagent.service.GmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gmail")
@RequiredArgsConstructor
public class GmailController {

    private final GmailService gmailService;
    private final EmailClassifierService emailClassifierService;

    @GetMapping("/auth-url")
    @RequirePermission("profile:read")
    public ResponseEntity<BaseResponse<Map<String, String>>> getAuthUrl() {
        String state = OAuthStateToken.generate(SecurityUtils.getCurrentUserId());
        String url = gmailService.getAuthorizationUrl(state);
        return ResponseEntity.ok(BaseResponse.success(Map.of("url", url)));
    }

    @GetMapping("/callback")
    public ResponseEntity<BaseResponse<?>> handleCallback(
            @RequestParam String code,
            @RequestParam String state) {
        UUID userId = OAuthStateToken.validate(state);
        if (userId == null) {
            return ResponseEntity.badRequest().body(BaseResponse.error("400", "INVALID_STATE", "Invalid or expired OAuth state"));
        }
        try {
            gmailService.handleCallback(userId, code);
            return ResponseEntity.ok(BaseResponse.success("Gmail connected", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(BaseResponse.error("500", "GMAIL_ERROR", "Failed to connect Gmail: " + e.getMessage()));
        }
    }

    @GetMapping("/status")
    @RequirePermission("profile:read")
    public ResponseEntity<BaseResponse<Map<String, Boolean>>> getStatus() {
        return ResponseEntity.ok(BaseResponse.success(Map.of("connected", gmailService.isConfigured())));
    }

    @PostMapping("/classify")
    @RequirePermission("applications:read")
    public ResponseEntity<BaseResponse<Map<String, Object>>> classifyEmails() throws Exception {
        UUID userId = SecurityUtils.getCurrentUserId();
        var emails = gmailService.fetchRecentEmails(userId, 20);
        return ResponseEntity.ok(BaseResponse.success(
                emailClassifierService.classifyAndProcess(userId, emails)));
    }
}
