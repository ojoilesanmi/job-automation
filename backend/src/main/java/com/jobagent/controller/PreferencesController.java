package com.jobagent.controller;

import com.jobagent.dto.*;
import com.jobagent.security.RequirePermission;
import com.jobagent.security.SecurityUtils;
import com.jobagent.service.PreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
public class PreferencesController {

    private final PreferencesService preferencesService;

    @GetMapping
    @RequirePermission("preferences:read")
    public ResponseEntity<BaseResponse<UserPreferencesResponse>> getPreferences() {
        return ResponseEntity.ok(BaseResponse.success(preferencesService.getPreferences(SecurityUtils.getCurrentUserId())));
    }

    @PutMapping
    @RequirePermission("preferences:write")
    public ResponseEntity<BaseResponse<UserPreferencesResponse>> updatePreferences(
            @Valid @RequestBody UpdatePreferencesRequest request) {
        return ResponseEntity.ok(BaseResponse.success("Preferences updated",
                preferencesService.updatePreferences(SecurityUtils.getCurrentUserId(), request)));
    }
}
