package com.jobagent.service;

import com.jobagent.dto.*;
import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.JobSource;
import com.jobagent.model.Permission;
import com.jobagent.model.Role;
import com.jobagent.model.User;
import com.jobagent.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final JobSourceRepository jobSourceRepository;

    // Roles
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::toRoleResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleResponse getRole(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        return toRoleResponse(role);
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Role name already exists");
        }

        Role role = Role.builder()
                .name(request.name())
                .description(request.description())
                .permissions(new HashSet<>())
                .build();

        if (request.permissions() != null) {
            Set<Permission> permissions = request.permissions().stream()
                    .map(name -> permissionRepository.findByName(name)
                            .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + name)))
                    .collect(Collectors.toSet());
            role.setPermissions(permissions);
        }

        role = roleRepository.save(role);
        return toRoleResponse(role);
    }

    @Transactional
    public RoleResponse updateRole(UUID roleId, CreateRoleRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (request.description() != null) role.setDescription(request.description());

        if (request.permissions() != null) {
            Set<Permission> permissions = request.permissions().stream()
                    .map(name -> permissionRepository.findByName(name)
                            .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + name)))
                    .collect(Collectors.toSet());
            role.setPermissions(permissions);
        }

        role = roleRepository.save(role);
        return toRoleResponse(role);
    }

    @Transactional
    public void deleteRole(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        if ("SUPER_ADMIN".equals(role.getName())) {
            throw new IllegalArgumentException("Cannot delete SUPER_ADMIN role");
        }
        roleRepository.delete(role);
    }

    // Permissions
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(p -> new PermissionResponse(p.getId(), p.getName(), p.getDescription(), p.getCreatedAt()))
                .collect(Collectors.toList());
    }

    // Users
    @Transactional(readOnly = true)
    public AdminUserListResponse getAllUsers(int page, int size) {
        Page<User> users = userRepository.findAll(PageRequest.of(page, size));
        List<AdminUserResponse> userList = users.getContent().stream()
                .map(this::toAdminUserResponse)
                .collect(Collectors.toList());
        return new AdminUserListResponse(userList, users.getTotalElements(), users.getTotalPages(), users.getNumber());
    }

    @Transactional
    public AdminUserResponse assignRole(UUID userId, AssignRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Role role = roleRepository.findByName(request.roleName())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.roleName()));

        user.getRoles().add(role);
        user = userRepository.save(user);
        return toAdminUserResponse(user);
    }

    @Transactional
    public AdminUserResponse removeRole(UUID userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        if ("SUPER_ADMIN".equals(roleName) && user.getRoles().stream().noneMatch(r -> "SUPER_ADMIN".equals(r.getName()) && !r.getId().equals(role.getId()))) {
            throw new IllegalArgumentException("Cannot remove the last SUPER_ADMIN role");
        }

        user.getRoles().remove(role);
        user = userRepository.save(user);
        return toAdminUserResponse(user);
    }

    // Job Sources
    @Transactional(readOnly = true)
    public List<JobSourceResponse> getAllJobSources() {
        return jobSourceRepository.findAll().stream()
                .map(this::toJobSourceResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public JobSourceResponse createJobSource(CreateJobSourceRequest request) {
        JobSource source = JobSource.builder()
                .name(request.name())
                .sourceType(request.sourceType())
                .baseUrl(request.baseUrl())
                .configJson(request.configJson())
                .enabled(true)
                .build();
        source = jobSourceRepository.save(source);
        return toJobSourceResponse(source);
    }

    @Transactional
    public JobSourceResponse updateJobSource(UUID sourceId, CreateJobSourceRequest request) {
        JobSource source = jobSourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Job source not found"));

        if (request.name() != null) source.setName(request.name());
        if (request.sourceType() != null) source.setSourceType(request.sourceType());
        if (request.baseUrl() != null) source.setBaseUrl(request.baseUrl());
        if (request.configJson() != null) source.setConfigJson(request.configJson());

        source = jobSourceRepository.save(source);
        return toJobSourceResponse(source);
    }

    @Transactional
    public void toggleJobSource(UUID sourceId) {
        JobSource source = jobSourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Job source not found"));
        source.setEnabled(!source.getEnabled());
        jobSourceRepository.save(source);
    }

    @Transactional
    public void deleteJobSource(UUID sourceId) {
        JobSource source = jobSourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Job source not found"));
        jobSourceRepository.delete(source);
    }

    private RoleResponse toRoleResponse(Role role) {
        Set<String> permissions = role.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(), permissions, role.getCreatedAt());
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getFullName(), roles, user.getCreatedAt());
    }

    private JobSourceResponse toJobSourceResponse(JobSource source) {
        return new JobSourceResponse(
                source.getId(), source.getName(), source.getSourceType(),
                source.getBaseUrl(), source.getEnabled(), source.getConfigJson(), source.getCreatedAt()
        );
    }
}
