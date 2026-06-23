package com.jobagent.controller;

import com.jobagent.dto.*;
import com.jobagent.model.Job;
import com.jobagent.security.RequireRole;
import com.jobagent.service.AdminService;
import com.jobagent.worker.JobDiscoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final JobDiscoveryService jobDiscoveryService;

    // Roles
    @GetMapping("/roles")
    @RequireRole("ADMIN")
    public ResponseEntity<BaseResponse<List<RoleResponse>>> getAllRoles() {
        return ResponseEntity.ok(BaseResponse.success(adminService.getAllRoles()));
    }

    @GetMapping("/roles/{id}")
    @RequireRole("ADMIN")
    public ResponseEntity<BaseResponse<RoleResponse>> getRole(@PathVariable UUID id) {
        return ResponseEntity.ok(BaseResponse.success(adminService.getRole(id)));
    }

    @PostMapping("/roles")
    @RequireRole("SUPER_ADMIN")
    public ResponseEntity<BaseResponse<RoleResponse>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("Role created", adminService.createRole(request)));
    }

    @PutMapping("/roles/{id}")
    @RequireRole("SUPER_ADMIN")
    public ResponseEntity<BaseResponse<RoleResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(BaseResponse.success("Role updated", adminService.updateRole(id, request)));
    }

    @DeleteMapping("/roles/{id}")
    @RequireRole("SUPER_ADMIN")
    public ResponseEntity<BaseResponse<Void>> deleteRole(@PathVariable UUID id) {
        adminService.deleteRole(id);
        return ResponseEntity.ok(BaseResponse.success("Role deleted", null));
    }

    // Permissions
    @GetMapping("/permissions")
    @RequireRole("ADMIN")
    public ResponseEntity<BaseResponse<List<PermissionResponse>>> getAllPermissions() {
        return ResponseEntity.ok(BaseResponse.success(adminService.getAllPermissions()));
    }

    // Users
    @GetMapping("/users")
    @RequireRole("ADMIN")
    public ResponseEntity<BaseResponse<AdminUserListResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(BaseResponse.success(adminService.getAllUsers(page, size)));
    }

    @PostMapping("/users/{userId}/roles")
    @RequireRole("SUPER_ADMIN")
    public ResponseEntity<BaseResponse<AdminUserResponse>> assignRole(
            @PathVariable UUID userId,
            @Valid @RequestBody AssignRoleRequest request) {
        return ResponseEntity.ok(BaseResponse.success("Role assigned", adminService.assignRole(userId, request)));
    }

    @DeleteMapping("/users/{userId}/roles/{roleName}")
    @RequireRole("SUPER_ADMIN")
    public ResponseEntity<BaseResponse<AdminUserResponse>> removeRole(
            @PathVariable UUID userId,
            @PathVariable String roleName) {
        return ResponseEntity.ok(BaseResponse.success("Role removed", adminService.removeRole(userId, roleName)));
    }

    // Job Sources
    @GetMapping("/job-sources")
    @RequireRole("ADMIN")
    public ResponseEntity<BaseResponse<List<JobSourceResponse>>> getAllJobSources() {
        return ResponseEntity.ok(BaseResponse.success(adminService.getAllJobSources()));
    }

    @PostMapping("/job-sources")
    @RequireRole("ADMIN")
    public ResponseEntity<BaseResponse<JobSourceResponse>> createJobSource(
            @Valid @RequestBody CreateJobSourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("Job source created", adminService.createJobSource(request)));
    }

    @PutMapping("/job-sources/{id}")
    @RequireRole("ADMIN")
    public ResponseEntity<BaseResponse<JobSourceResponse>> updateJobSource(
            @PathVariable UUID id,
            @Valid @RequestBody CreateJobSourceRequest request) {
        return ResponseEntity.ok(BaseResponse.success("Job source updated", adminService.updateJobSource(id, request)));
    }

    @PatchMapping("/job-sources/{id}/toggle")
    @RequireRole("ADMIN")
    public ResponseEntity<BaseResponse<Void>> toggleJobSource(@PathVariable UUID id) {
        adminService.toggleJobSource(id);
        return ResponseEntity.ok(BaseResponse.success("Job source toggled", null));
    }

    @DeleteMapping("/job-sources/{id}")
    @RequireRole("ADMIN")
    public ResponseEntity<BaseResponse<Void>> deleteJobSource(@PathVariable UUID id) {
        adminService.deleteJobSource(id);
        return ResponseEntity.ok(BaseResponse.success("Job source deleted", null));
    }

    // Job Discovery
    @PostMapping("/job-sources/{id}/discover")
    @RequireRole("ADMIN")
    public ResponseEntity<BaseResponse<List<Job>>> triggerDiscovery(@PathVariable UUID id) {
        List<Job> jobs = jobDiscoveryService.triggerManualDiscovery(id);
        return ResponseEntity.ok(BaseResponse.success("Discovery complete, fetched " + jobs.size() + " jobs", jobs));
    }

    @GetMapping("/discovery/connectors")
    @RequireRole("ADMIN")
    public ResponseEntity<BaseResponse<Map<String, String>>> getConnectors() {
        return ResponseEntity.ok(BaseResponse.success(jobDiscoveryService.getAvailableConnectors()));
    }
}
