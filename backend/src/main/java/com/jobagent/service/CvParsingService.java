package com.jobagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobagent.model.*;
import com.jobagent.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CvParsingService {

    private final CvDocumentRepository cvDocumentRepository;
    private final UserProfileRepository profileRepository;
    private final ProfileSkillRepository skillRepository;
    private final WorkExperienceRepository experienceRepository;
    private final FileStorageService fileStorageService;
    private final AiServiceClient aiServiceClient;

    @Async
    @Transactional
    public void parseCvAndPopulateProfile(UUID userId, UUID cvId) {
        try {
            CvDocument cv = cvDocumentRepository.findByIdAndUserId(cvId, userId).orElse(null);
            if (cv == null) return;

            String fileType = cv.getFileName().toLowerCase().endsWith(".pdf") ? "pdf" : "docx";
            JsonNode parsed = aiServiceClient.parseCvContent(fileStorageService.retrieve(cv.getFileUrl()), fileType);
            if (parsed == null) return;

            cv.setParsedText(parsed.path("rawText").asText(null));
            cvDocumentRepository.save(cv);

            UserProfile profile = profileRepository.findByUserId(userId).orElseGet(() -> {
                User user = new User();
                user.setId(userId);
                return UserProfile.builder().user(user).build();
            });

            String fullName = parsed.path("fullName").asText("");
            if (!fullName.isBlank() && profile.getHeadline() == null) profile.setHeadline(fullName);
            String summary = parsed.path("summary").asText("");
            if (!summary.isBlank() && profile.getSummary() == null) profile.setSummary(summary);
            String location = parsed.path("location").asText("");
            if (!location.isBlank() && profile.getLocation() == null) profile.setLocation(location);
            profileRepository.save(profile);

            populateSkills(userId, parsed.path("skills"));
            populateExperience(userId, parsed.path("workExperience"));

            log.info("CV parsed and profile auto-populated for user {}", userId);
        } catch (Exception e) {
            log.warn("CV auto-parse failed for user {}: {}", userId, e.getMessage());
        }
    }

    private void populateSkills(UUID userId, JsonNode skillsNode) {
        if (!skillsNode.isArray() || !skillRepository.findByUserId(userId).isEmpty()) return;
        User user = new User();
        user.setId(userId);
        List<ProfileSkill> skills = new ArrayList<>();
        for (JsonNode s : skillsNode) {
            String name = s.asText("").trim();
            if (!name.isBlank()) {
                skills.add(ProfileSkill.builder().user(user).skillName(name).skillType("technical").build());
            }
        }
        skillRepository.saveAll(skills);
    }

    private void populateExperience(UUID userId, JsonNode experienceNode) {
        if (!experienceNode.isArray() || !experienceRepository.findByUserIdOrderByStartDateDesc(userId).isEmpty()) return;
        User user = new User();
        user.setId(userId);
        List<WorkExperience> exps = new ArrayList<>();
        for (JsonNode e : experienceNode) {
            String company = e.path("company").asText("");
            String title = e.path("title").asText("");
            if (company.isBlank() && title.isBlank()) continue;
            WorkExperience exp = WorkExperience.builder()
                    .user(user)
                    .company(company)
                    .title(title)
                    .description(e.path("description").asText(""))
                    .build();
            setDate(exp, e.path("startDate").asText(null), true);
            setDate(exp, e.path("endDate").asText(null), false);
            exps.add(exp);
        }
        experienceRepository.saveAll(exps);
    }

    private void setDate(WorkExperience exp, String value, boolean start) {
        if (value == null || value.equalsIgnoreCase("Present") || value.equalsIgnoreCase("Current")) return;
        try {
            LocalDate parsed = LocalDate.parse(value);
            if (start) exp.setStartDate(parsed); else exp.setEndDate(parsed);
        } catch (Exception ignored) {
        }
    }
}
