package com.jobagent.service;

import com.jobagent.dto.SubmissionChecklistResponse;
import com.jobagent.exception.ForbiddenException;
import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.*;
import com.jobagent.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationSubmissionService {

    private final ApplicationRepository applicationRepository;
    private final SubmissionLogRepository submissionLogRepository;
    private final JobMatchRepository matchRepository;
    private final CvDocumentRepository cvDocumentRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final UserPreferencesRepository preferencesRepository;

    @Transactional(readOnly = true)
    public SubmissionChecklistResponse getSubmissionChecklist(UUID userId, UUID applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (!app.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }

        List<SubmissionChecklistResponse.CheckItem> items = new ArrayList<>();

        boolean hasCv = app.getCvDocument() != null;
        items.add(new SubmissionChecklistResponse.CheckItem("cv_attached", hasCv,
                hasCv ? "CV is attached" : "No CV attached"));

        boolean hasCl = app.getCoverLetter() != null;
        items.add(new SubmissionChecklistResponse.CheckItem("cover_letter_generated", hasCl,
                hasCl ? "Cover letter is generated" : "No cover letter generated"));

        boolean hasMatch = matchRepository.findByUserIdAndJobId(userId, app.getJob().getId()).isPresent();
        JobMatch match = hasMatch ? matchRepository.findByUserIdAndJobId(userId, app.getJob().getId()).get() : null;
        boolean scorePassed = match != null && match.getFitScore().compareTo(BigDecimal.valueOf(70)) >= 0;
        items.add(new SubmissionChecklistResponse.CheckItem("fit_score_passed", scorePassed,
                scorePassed ? "Fit score: " + (match != null ? match.getFitScore() : 0) + "%" : "Fit score below threshold"));

        boolean duplicateCheck = applicationRepository.countOtherApplicationsForJob(userId, app.getJob().getId(), app.getId()) == 0;
        items.add(new SubmissionChecklistResponse.CheckItem("duplicate_check_passed", duplicateCheck,
                duplicateCheck ? "Not a duplicate application" : "Duplicate application found for this job"));

        boolean hasUrl = app.getJob().getApplicationUrl() != null && !app.getJob().getApplicationUrl().isBlank();
        items.add(new SubmissionChecklistResponse.CheckItem("application_url_available", hasUrl,
                hasUrl ? "Application URL is available" : "No application URL found"));

        boolean statusReady = "approved".equals(app.getStatus()) || "pending_approval".equals(app.getStatus());
        items.add(new SubmissionChecklistResponse.CheckItem("status_ready", statusReady,
                statusReady ? "Application is ready for submission" : "Status: " + app.getStatus()));

        boolean allPassed = items.stream().allMatch(SubmissionChecklistResponse.CheckItem::passed);
        return new SubmissionChecklistResponse(allPassed, items);
    }

    @Transactional(readOnly = true)
    public List<SubmissionLog> getSubmissionLogs(UUID userId, UUID applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (!app.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }
        return submissionLogRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId);
    }
}
