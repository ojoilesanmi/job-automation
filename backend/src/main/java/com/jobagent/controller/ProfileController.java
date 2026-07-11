package com.jobagent.controller;

import com.jobagent.dto.*;
import com.jobagent.security.RequirePermission;
import com.jobagent.security.SecurityUtils;
import com.jobagent.service.ProfileDataService;
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
    private final ProfileDataService profileDataService;

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

    @GetMapping("/education")
    @RequirePermission("profile:read")
    public ResponseEntity<BaseResponse<List<EducationResponse>>> getEducation() {
        return ResponseEntity.ok(BaseResponse.success(
                profileDataService.getEducation(SecurityUtils.getCurrentUserId())));
    }

    @PostMapping("/education")
    @RequirePermission("profile:write")
    public ResponseEntity<BaseResponse<List<EducationResponse>>> saveEducation(@RequestBody SaveEducationRequest request) {
        return ResponseEntity.ok(BaseResponse.success("Education saved",
                profileDataService.saveEducation(SecurityUtils.getCurrentUserId(), request)));
    }

    @GetMapping("/certifications")
    @RequirePermission("profile:read")
    public ResponseEntity<BaseResponse<List<CertificationResponse>>> getCertifications() {
        return ResponseEntity.ok(BaseResponse.success(
                profileDataService.getCertifications(SecurityUtils.getCurrentUserId())));
    }

    @PostMapping("/certifications")
    @RequirePermission("profile:write")
    public ResponseEntity<BaseResponse<List<CertificationResponse>>> saveCertifications(@RequestBody SaveCertificationRequest request) {
        return ResponseEntity.ok(BaseResponse.success("Certifications saved",
                profileDataService.saveCertifications(SecurityUtils.getCurrentUserId(), request)));
    }

    @GetMapping("/links")
    @RequirePermission("profile:read")
    public ResponseEntity<BaseResponse<List<ProfileLinkResponse>>> getLinks() {
        return ResponseEntity.ok(BaseResponse.success(
                profileDataService.getLinks(SecurityUtils.getCurrentUserId())));
    }

    @PostMapping("/links")
    @RequirePermission("profile:write")
    public ResponseEntity<BaseResponse<List<ProfileLinkResponse>>> saveLinks(@RequestBody SaveLinksRequest request) {
        return ResponseEntity.ok(BaseResponse.success("Links saved",
                profileDataService.saveLinks(SecurityUtils.getCurrentUserId(), request)));
    }
}
