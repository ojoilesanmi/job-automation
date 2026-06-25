package com.jobagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobagent.dto.*;
import com.jobagent.exception.ForbiddenException;
import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.*;
import com.jobagent.repository.*;
import com.jobagent.security.SecurityUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoverLetterService {

    private final CoverLetterRepository coverLetterRepository;
    private final JobRepository jobRepository;
    private final CvDocumentRepository cvDocumentRepository;
    private final ProfileSkillRepository skillRepository;
    private final WorkExperienceRepository experienceRepository;
    private final UserProfileRepository profileRepository;
    private final AiServiceClient aiServiceClient;
    private final MeterRegistry meterRegistry;

    @Transactional
    public CoverLetterResponse generateCoverLetter(UUID userId, GenerateCoverLetterRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        Job job = jobRepository.findById(request.jobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        User user = new User();
        user.setId(userId);

        CvDocument cv = null;
        if (request.cvDocumentId() != null) {
            cv = cvDocumentRepository.findByIdAndUserId(request.cvDocumentId(), userId).orElse(null);
        }
        if (cv == null) {
            cv = cvDocumentRepository.findByUserIdAndIsDefaultTrue(userId).orElse(null);
        }

        List<ProfileSkill> skills = skillRepository.findByUserId(userId);
        List<WorkExperience> experiences = experienceRepository.findByUserIdOrderByStartDateDesc(userId);
        UserProfile profile = profileRepository.findByUserId(userId).orElse(null);

        String content = generateWithAiOrFallback(profile, skills, experiences, cv, job, request.tone());

        CoverLetter coverLetter = CoverLetter.builder()
                .user(user)
                .job(job)
                .cvDocument(cv)
                .content(content)
                .version(1)
                .status("draft")
                .build();

        coverLetter = coverLetterRepository.save(coverLetter);
        sample.stop(Timer.builder("jobagent.cover_letters.generation_time")
                .description("Time to generate cover letters")
                .register(meterRegistry));
        return toResponse(coverLetter, job);
    }

    private String generateWithAiOrFallback(UserProfile profile, List<ProfileSkill> skills,
                                             List<WorkExperience> experiences, CvDocument cv,
                                             Job job, String tone) {
        try {
            Map<String, Object> aiRequest = new LinkedHashMap<>();
            aiRequest.put("jobTitle", job.getTitle());
            aiRequest.put("company", job.getCompany());
            aiRequest.put("jobDescription", job.getDescription() != null ? job.getDescription() : "");

            if (profile != null) {
                Map<String, Object> profileData = new LinkedHashMap<>();
                profileData.put("name", profile.getHeadline() != null ? profile.getHeadline() : "Applicant");
                profileData.put("summary", profile.getSummary() != null ? profile.getSummary() : "");
                aiRequest.put("profile", profileData);
            }

            Map<String, Object> cvData = new LinkedHashMap<>();
            if (cv != null && cv.getParsedText() != null) {
                cvData.put("parsedText", cv.getParsedText());
            } else {
                cvData.put("parsedText", buildProfileText(skills, experiences));
            }
            aiRequest.put("cv", cvData);

            aiRequest.put("tone", tone != null ? tone : "professional");

            JsonNode aiResult = aiServiceClient.generateCoverLetter(aiRequest);
            if (aiResult != null && aiResult.has("content")) {
                return aiResult.get("content").asText();
            }
        } catch (Exception e) {
            log.warn("AI cover letter generation failed, falling back to template: {}", e.getMessage());
        }

        return buildCoverLetterContent(profile, skills, experiences, job, tone);
    }

    private String buildProfileText(List<ProfileSkill> skills, List<WorkExperience> experiences) {
        StringBuilder sb = new StringBuilder();
        if (!skills.isEmpty()) {
            sb.append("Skills: ");
            sb.append(String.join(", ", skills.stream().limit(10).map(ProfileSkill::getSkillName).toList()));
        }
        if (!experiences.isEmpty()) {
            sb.append("\nExperience: ");
            for (WorkExperience exp : experiences.stream().limit(3).toList()) {
                sb.append(exp.getTitle()).append(" at ").append(exp.getCompany()).append("; ");
            }
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public CoverLetterListResponse getCoverLetters(UUID userId, UUID jobId) {
        List<CoverLetter> letters = coverLetterRepository.findByUserIdAndJobIdOrderByVersionDesc(userId, jobId);
        List<CoverLetterResponse> responses = letters.stream()
                .map(cl -> toResponse(cl, cl.getJob()))
                .collect(Collectors.toList());
        return new CoverLetterListResponse(responses);
    }

    @Transactional(readOnly = true)
    public CoverLetterListResponse getAllCoverLetters(UUID userId) {
        List<CoverLetter> letters = coverLetterRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<CoverLetterResponse> responses = letters.stream()
                .map(cl -> toResponse(cl, cl.getJob()))
                .collect(Collectors.toList());
        return new CoverLetterListResponse(responses);
    }

    @Transactional
    public CoverLetterResponse updateCoverLetter(UUID userId, UUID coverLetterId, String content) {
        CoverLetter coverLetter = coverLetterRepository.findById(coverLetterId)
                .orElseThrow(() -> new ResourceNotFoundException("Cover letter not found"));
        assertOwnership(coverLetter, userId);

        coverLetter.setContent(content);
        coverLetter.setVersion(coverLetter.getVersion() + 1);
        coverLetter = coverLetterRepository.save(coverLetter);
        return toResponse(coverLetter, coverLetter.getJob());
    }

    @Transactional
    public CoverLetterResponse regenerateCoverLetter(UUID userId, UUID coverLetterId) {
        CoverLetter existing = coverLetterRepository.findById(coverLetterId)
                .orElseThrow(() -> new ResourceNotFoundException("Cover letter not found"));
        assertOwnership(existing, userId);

        GenerateCoverLetterRequest request = new GenerateCoverLetterRequest(
                existing.getJob().getId(),
                existing.getCvDocument() != null ? existing.getCvDocument().getId() : null,
                null
        );

        return generateCoverLetter(userId, request);
    }

    private String buildCoverLetterContent(UserProfile profile, List<ProfileSkill> skills,
                                            List<WorkExperience> experiences, Job job, String tone) {
        StringBuilder sb = new StringBuilder();

        sb.append("Dear Hiring Manager,\n\n");
        sb.append("I am writing to express my strong interest in the ").append(job.getTitle());
        sb.append(" position at ").append(job.getCompany()).append(". ");

        if (profile != null && profile.getSummary() != null) {
            sb.append(profile.getSummary()).append(" ");
        }

        if (!skills.isEmpty()) {
            String topSkills = skills.stream()
                    .limit(5)
                    .map(ProfileSkill::getSkillName)
                    .collect(Collectors.joining(", "));
            sb.append("\n\nMy technical expertise includes ").append(topSkills);
            sb.append(", which aligns well with the requirements of this role. ");
        }

        if (!experiences.isEmpty()) {
            WorkExperience latest = experiences.get(0);
            sb.append("\n\nIn my recent role as ").append(latest.getTitle());
            sb.append(" at ").append(latest.getCompany()).append(", ");
            if (latest.getDescription() != null) {
                sb.append(latest.getDescription()).append(" ");
            }
        }

        if (job.getDescription() != null && !job.getDescription().isBlank()) {
            String[] keywords = extractKeywords(job.getDescription());
            if (keywords.length > 0) {
                sb.append("\n\nI am particularly excited about ");
                sb.append(String.join(" and ", keywords));
                sb.append(", and I am confident my background would allow me to contribute meaningfully.");
            }
        }

        sb.append("\n\nI would welcome the opportunity to discuss how my skills and experience ");
        sb.append("align with your team's needs. Thank you for your consideration.\n\n");
        sb.append("Best regards,\n");
        sb.append(profile != null && profile.getLocation() != null ? profile.getLocation() : "Applicant");

        return sb.toString();
    }

    private String[] extractKeywords(String description) {
        String lower = description.toLowerCase();
        String[] techKeywords = {"python", "java", "javascript", "typescript", "react", "node",
                "aws", "docker", "kubernetes", "api", "microservices", "database", "sql"};
        List<String> found = new java.util.ArrayList<>();
        for (String kw : techKeywords) {
            if (lower.contains(kw)) found.add(kw);
            if (found.size() >= 3) break;
        }
        return found.toArray(new String[0]);
    }

    private void assertOwnership(CoverLetter coverLetter, UUID userId) {
        if (!coverLetter.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this cover letter");
        }
    }

    private CoverLetterResponse toResponse(CoverLetter cl, Job job) {
        return new CoverLetterResponse(
                cl.getId(), job.getId(), job.getTitle(), job.getCompany(),
                cl.getCvDocument() != null ? cl.getCvDocument().getId() : null,
                cl.getContent(), cl.getVersion(), cl.getStatus(),
                cl.getCreatedAt(), cl.getUpdatedAt()
        );
    }
}
