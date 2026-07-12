package com.jobagent.service;

import com.jobagent.dto.*;
import com.jobagent.exception.ForbiddenException;
import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.*;
import com.jobagent.repository.*;
import com.jobagent.security.SecurityUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private Set<String> allowedAppTransitions;

    private final ApplicationRepository applicationRepository;
    private final ApplicationEventRepository eventRepository;
    private final JobRepository jobRepository;
    private final CvDocumentRepository cvDocumentRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final AuditLogRepository auditLogRepository;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void init(@Value("${app.application.allowed-transitions}") String transitions) {
        this.allowedAppTransitions = new HashSet<>(Arrays.asList(transitions.split(",")));
    }

    @Transactional
    public ApplicationResponse createApplication(UUID userId, CreateApplicationRequest request) {
        Job job = jobRepository.findById(request.jobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        User user = new User();
        user.setId(userId);

        Application existing = applicationRepository.findByUserIdAndJobId(userId, request.jobId()).orElse(null);
        if (existing != null) {
            throw new IllegalArgumentException("Application already exists for this job");
        }

        Application app = Application.builder()
                .user(user)
                .job(job)
                .applicationMode(request.applicationMode() != null ? request.applicationMode() : "approval")
                .status("pending_approval")
                .build();

        if (request.cvDocumentId() != null) {
            CvDocument cv = cvDocumentRepository.findByIdAndUserId(request.cvDocumentId(), userId).orElse(null);
            app.setCvDocument(cv);
        }
        if (request.coverLetterId() != null) {
            CoverLetter cl = coverLetterRepository.findById(request.coverLetterId()).orElse(null);
            app.setCoverLetter(cl);
        }

        app = applicationRepository.save(app);
        logAudit(userId, "application.created", "application", app.getId());
        return toResponse(app);
    }

    @Transactional(readOnly = true)
    public ApplicationListResponse getApplications(UUID userId, String status, int page, int size) {
        Page<Application> apps;
        if (status != null && !status.isEmpty()) {
            apps = applicationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status,
                    PageRequest.of(page, size));
        } else {
            apps = applicationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        }

        List<ApplicationResponse> responses = apps.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new ApplicationListResponse(responses, apps.getTotalElements(), apps.getTotalPages(), apps.getNumber());
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(UUID userId, UUID appId) {
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        assertOwnership(app, userId);
        return toResponse(app);
    }

    @Transactional
    public ApplicationResponse updateStatus(UUID userId, UUID appId, String newStatus) {
        if (!allowedAppTransitions.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid application status: " + newStatus);
        }
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        assertOwnership(app, userId);

        String oldStatus = app.getStatus();
        app.setStatus(newStatus);

        if ("submitted".equals(newStatus)) {
            app.setSubmittedAt(OffsetDateTime.now());
        }

        app = applicationRepository.save(app);

        ApplicationEvent event = ApplicationEvent.builder()
                .application(app)
                .eventType("status_changed")
                .eventPayload(Map.of("from", oldStatus, "to", newStatus))
                .build();
        eventRepository.save(event);

        logAudit(userId, "application.status_changed", "application", app.getId(),
                Map.of("from", oldStatus, "to", newStatus));
        return toResponse(app);
    }

    @Transactional
    public ApplicationResponse submitApplication(UUID userId, UUID appId) {
        ApplicationResponse response = updateStatus(userId, appId, "submitted");
        Counter.builder("jobagent.applications.submitted")
                .description("Number of applications submitted")
                .register(meterRegistry)
                .increment();
        return response;
    }

    @Transactional
    public ApplicationResponse addNote(UUID userId, UUID appId, String notes) {
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        assertOwnership(app, userId);
        app.setNotes(notes);
        app = applicationRepository.save(app);
        return toResponse(app);
    }

    private void assertOwnership(Application app, UUID userId) {
        if (!app.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this application");
        }
    }

    private void logAudit(UUID userId, String action, String entityType, UUID entityId) {
        logAudit(userId, action, entityType, entityId, null);
    }

    private void logAudit(UUID userId, String action, String entityType, UUID entityId,
                           Map<String, Object> metadata) {
        User user = new User();
        user.setId(userId);
        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .metadata(metadata)
                .build();
        auditLogRepository.save(log);
    }

    private ApplicationResponse toResponse(Application app) {
        return new ApplicationResponse(
                app.getId(), app.getJob().getId(), app.getJob().getTitle(), app.getJob().getCompany(),
                app.getCvDocument() != null ? app.getCvDocument().getId() : null,
                app.getCoverLetter() != null ? app.getCoverLetter().getId() : null,
                app.getStatus(), app.getApplicationMode(),
                app.getSubmittedAt(), app.getSourceUrl(), app.getNotes(),
                app.getLastFollowUpAt(), app.getNextFollowUpAt(), app.getCreatedAt()
        );
    }
}
