package com.jobagent.controller;

import com.jobagent.dto.*;
import com.jobagent.security.RequirePermission;
import com.jobagent.security.SecurityUtils;
import com.jobagent.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/profile")
    @RequirePermission("profile:read")
    public ResponseEntity<BaseResponse<UserProfileResponse>> getProfile() {
        return ResponseEntity.ok(BaseResponse.success(profileService.getProfile(SecurityUtils.getCurrentUserId())));
    }

    @PutMapping("/profile")
    @RequirePermission("profile:write")
    public ResponseEntity<BaseResponse<UserProfileResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(BaseResponse.success("Profile updated", profileService.updateProfile(SecurityUtils.getCurrentUserId(), request)));
    }

    @PutMapping("/profile/skills")
    @RequirePermission("profile:write")
    public ResponseEntity<BaseResponse<Void>> updateSkills(@Valid @RequestBody UpdateSkillsRequest request) {
        profileService.updateSkills(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(BaseResponse.success("Skills updated successfully", null));
    }

    @PutMapping("/profile/experience")
    @RequirePermission("profile:write")
    public ResponseEntity<BaseResponse<Void>> updateExperience(@Valid @RequestBody UpdateExperienceRequest request) {
        profileService.updateExperience(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(BaseResponse.success("Experience updated successfully", null));
    }

    @PutMapping("/profile/projects")
    @RequirePermission("profile:write")
    public ResponseEntity<BaseResponse<Void>> updateProjects(@Valid @RequestBody UpdateProjectsRequest request) {
        profileService.updateProjects(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(BaseResponse.success("Projects updated successfully", null));
    }

    @GetMapping("/cvs")
    @RequirePermission("cv:read")
    public ResponseEntity<BaseResponse<List<CvDocumentResponse>>> listCvs() {
        return ResponseEntity.ok(BaseResponse.success(profileService.listCvs(SecurityUtils.getCurrentUserId())));
    }

    @GetMapping("/cvs/{id}")
    @RequirePermission("cv:read")
    public ResponseEntity<BaseResponse<CvDocumentResponse>> getCv(@PathVariable UUID id) {
        return ResponseEntity.ok(BaseResponse.success(profileService.getCv(SecurityUtils.getCurrentUserId(), id)));
    }

    @PostMapping(value = "/cvs/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("cv:write")
    public ResponseEntity<BaseResponse<CvDocumentResponse>> uploadCv(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("CV uploaded successfully",
                        profileService.uploadCv(SecurityUtils.getCurrentUserId(), file)));
    }

    @PutMapping("/cvs/{id}/default")
    @RequirePermission("cv:write")
    public ResponseEntity<BaseResponse<Void>> setDefaultCv(@PathVariable UUID id) {
        profileService.setDefaultCv(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(BaseResponse.success("Default CV updated", null));
    }

    @DeleteMapping("/cvs/{id}")
    @RequirePermission("cv:write")
    public ResponseEntity<BaseResponse<Void>> deleteCv(@PathVariable UUID id) {
        profileService.deleteCv(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(BaseResponse.success("CV deleted", null));
    }
}
