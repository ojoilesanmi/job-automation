package com.jobagent.controller;

import com.jobagent.dto.*;
import com.jobagent.security.RequirePermission;
import com.jobagent.security.SecurityUtils;
import com.jobagent.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @RequirePermission("profile:read")
    public ResponseEntity<BaseResponse<NotificationListResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(BaseResponse.success(
                notificationService.getNotifications(SecurityUtils.getCurrentUserId(), page, size)));
    }

    @GetMapping("/unread-count")
    @RequirePermission("profile:read")
    public ResponseEntity<BaseResponse<Map<String, Long>>> getUnreadCount() {
        long count = notificationService.getUnreadCount(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(BaseResponse.success(Map.of("count", count)));
    }

    @PostMapping("/{id}/read")
    @RequirePermission("profile:write")
    public ResponseEntity<BaseResponse<Void>> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(BaseResponse.success("Marked as read", null));
    }

    @PostMapping("/read-all")
    @RequirePermission("profile:write")
    public ResponseEntity<BaseResponse<Void>> markAllAsRead() {
        notificationService.markAllAsRead(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(BaseResponse.success("All marked as read", null));
    }
}
