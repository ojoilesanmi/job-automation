package com.jobagent.service;

import com.jobagent.dto.*;
import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.*;
import com.jobagent.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserProfileRepository profileRepository;
    private final CvDocumentRepository cvDocumentRepository;
    private final ProfileSkillRepository skillRepository;
    private final WorkExperienceRepository experienceRepository;
    private final ProjectRepository projectRepository;
    private final FileStorageService fileStorageService;
    private final AiServiceClient aiServiceClient;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        List<ProfileSkill> skills = skillRepository.findByUserId(userId);
        List<WorkExperience> experiences = experienceRepository.findByUserIdOrderByStartDateDesc(userId);
        List<Project> projects = projectRepository.findByUserId(userId);

        return new UserProfileResponse(
                profile.getId(),
                profile.getHeadline(),
                profile.getSummary(),
                profile.getLocation(),
                profile.getYearsOfExperience(),
                profile.getPrimaryRole(),
                profile.getIndustries(),
                profile.getTonePreference(),
                skills.stream().map(s -> new UserProfileResponse.SkillDto(
                        s.getId(), s.getSkillName(), s.getSkillType(), s.getProficiency(), s.getYearsUsed()
                )).collect(Collectors.toList()),
                experiences.stream().map(e -> new UserProfileResponse.WorkExperienceDto(
                        e.getId(), e.getCompany(), e.getTitle(),
                        e.getStartDate() != null ? e.getStartDate().toString() : null,
                        e.getEndDate() != null ? e.getEndDate().toString() : null,
                        e.getDescription(), e.getAchievements()
                )).collect(Collectors.toList()),
                projects.stream().map(p -> new UserProfileResponse.ProjectDto(
                        p.getId(), p.getName(), p.getDescription(), p.getTechnologies(), p.getUrl()
                )).collect(Collectors.toList()),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        if (request.headline() != null) profile.setHeadline(request.headline());
        if (request.summary() != null) profile.setSummary(request.summary());
        if (request.location() != null) profile.setLocation(request.location());
        if (request.yearsOfExperience() != null) profile.setYearsOfExperience(request.yearsOfExperience());
        if (request.primaryRole() != null) profile.setPrimaryRole(request.primaryRole());
        if (request.industries() != null) profile.setIndustries(request.industries());
        if (request.tonePreference() != null) profile.setTonePreference(request.tonePreference());

        profileRepository.save(profile);
        return getProfile(userId);
    }

    @Transactional
    public void updateSkills(UUID userId, UpdateSkillsRequest request) {
        skillRepository.deleteByUserId(userId);
        User user = new User();
        user.setId(userId);

        List<ProfileSkill> skills = new ArrayList<>();
        for (UpdateSkillsRequest.SkillItem item : request.skills()) {
            ProfileSkill skill = ProfileSkill.builder()
                    .user(user)
                    .skillName(item.skillName())
                    .skillType(item.skillType() != null ? item.skillType() : "technical")
                    .proficiency(item.proficiency())
                    .yearsUsed(item.yearsUsed())
                    .build();
            skills.add(skill);
        }
        skillRepository.saveAll(skills);
    }

    @Transactional
    public void updateExperience(UUID userId, UpdateExperienceRequest request) {
        experienceRepository.deleteByUserId(userId);
        User user = new User();
        user.setId(userId);

        List<WorkExperience> experiences = new ArrayList<>();
        for (UpdateExperienceRequest.ExperienceItem item : request.experiences()) {
            WorkExperience exp = WorkExperience.builder()
                    .user(user)
                    .company(item.company())
                    .title(item.title())
                    .startDate(item.startDate() != null ? LocalDate.parse(item.startDate()) : null)
                    .endDate(item.endDate() != null ? LocalDate.parse(item.endDate()) : null)
                    .description(item.description())
                    .achievements(item.achievements())
                    .build();
            experiences.add(exp);
        }
        experienceRepository.saveAll(experiences);
    }

    @Transactional
    public void updateProjects(UUID userId, UpdateProjectsRequest request) {
        projectRepository.deleteByUserId(userId);
        User user = new User();
        user.setId(userId);

        List<Project> projects = new ArrayList<>();
        for (UpdateProjectsRequest.ProjectItem item : request.projects()) {
            Project project = Project.builder()
                    .user(user)
                    .name(item.name())
                    .description(item.description())
                    .technologies(item.technologies())
                    .url(item.url())
                    .build();
            projects.add(project);
        }
        projectRepository.saveAll(projects);
    }

    @Transactional(readOnly = true)
    public List<CvDocumentResponse> listCvs(UUID userId) {
        return cvDocumentRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream()
                .map(this::toCvResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CvDocumentResponse getCv(UUID userId, UUID cvId) {
        CvDocument cv = cvDocumentRepository.findByIdAndUserId(cvId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("CV not found"));
        return toCvResponse(cv);
    }

    @Transactional(readOnly = true)
    public UUID getDefaultCvId(UUID userId) {
        return cvDocumentRepository.findByUserIdAndIsDefaultTrue(userId)
                .map(CvDocument::getId)
                .orElse(null);
    }

    @Transactional
    public CvDocumentResponse uploadCv(UUID userId, MultipartFile file) {
        User user = new User();
        user.setId(userId);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "untitled_cv";
        }

        String fileUrl;
        try {
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            fileUrl = fileStorageService.store("cvs/" + userId, originalFilename, file.getBytes(), contentType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CV file: " + e.getMessage(), e);
        }

        boolean hasExistingCvs = !cvDocumentRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).isEmpty();

        CvDocument cv = CvDocument.builder()
                .user(user)
                .fileName(originalFilename)
                .fileUrl(fileUrl)
                .parsedText(null)
                .versionName(originalFilename)
                .isDefault(!hasExistingCvs)
                .build();

        cv = cvDocumentRepository.save(cv);

        parseCvAndPopulateProfile(userId, cv);

        return toCvResponse(cv);
    }

    @Async
    public void parseCvAndPopulateProfile(UUID userId, CvDocument cv) {
        try {
            String fileType = cv.getFileName().toLowerCase().endsWith(".pdf") ? "pdf" : "docx";
            JsonNode parsed = aiServiceClient.parseCv(cv.getFileUrl(), fileType);
            if (parsed == null) return;

            cv.setParsedText(parsed.has("rawText") ? parsed.get("rawText").asText() : null);
            cvDocumentRepository.save(cv);

            UserProfile profile = profileRepository.findByUserId(userId).orElse(null);
            if (profile == null) {
                User user = new User();
                user.setId(userId);
                profile = UserProfile.builder().user(user).build();
            }

            if (parsed.has("name") && !parsed.get("name").asText().isBlank()) {
                String fullName = parsed.get("name").asText();
                if (profile.getHeadline() == null) profile.setHeadline(fullName);
            }
            if (parsed.has("summary") && !parsed.get("summary").asText().isBlank()) {
                if (profile.getSummary() == null) profile.setSummary(parsed.get("summary").asText());
            }
            if (parsed.has("location") && !parsed.get("location").asText().isBlank()) {
                if (profile.getLocation() == null) profile.setLocation(parsed.get("location").asText());
            }
            if (parsed.has("yearsExperience")) {
                int years = parsed.get("yearsExperience").asInt(0);
                if (years > 0 && profile.getYearsOfExperience() == null) {
                    profile.setYearsOfExperience(years);
                }
            }

            profile = profileRepository.save(profile);

            if (parsed.has("skills") && parsed.get("skills").isArray()) {
                List<ProfileSkill> existing = skillRepository.findByUserId(userId);
                if (existing.isEmpty()) {
                    User user = new User();
                    user.setId(userId);
                    List<ProfileSkill> skills = new ArrayList<>();
                    for (JsonNode s : parsed.get("skills")) {
                        String name = s.isTextual() ? s.asText() : s.path("name").asText("");
                        if (!name.isBlank()) {
                            skills.add(ProfileSkill.builder()
                                    .user(user)
                                    .skillName(name)
                                    .skillType("technical")
                                    .build());
                        }
                    }
                    skillRepository.saveAll(skills);
                }
            }

            if (parsed.has("experience") && parsed.get("experience").isArray()) {
                List<WorkExperience> existing = experienceRepository.findByUserIdOrderByStartDateDesc(userId);
                if (existing.isEmpty()) {
                    User user = new User();
                    user.setId(userId);
                    List<WorkExperience> exps = new ArrayList<>();
                    for (JsonNode e : parsed.get("experience")) {
                        String company = e.path("company").asText("");
                        String title = e.path("title").asText("");
                        if (!company.isBlank() || !title.isBlank()) {
                            WorkExperience exp = WorkExperience.builder()
                                    .user(user)
                                    .company(company)
                                    .title(title)
                                    .description(e.path("description").asText(""))
                                    .build();
                            String startStr = e.path("startDate").asText(null);
                            if (startStr != null) {
                                try { exp.setStartDate(LocalDate.parse(startStr)); } catch (Exception ignored) {}
                            }
                            String endStr = e.path("endDate").asText(null);
                            if (endStr != null) {
                                try { exp.setEndDate(LocalDate.parse(endStr)); } catch (Exception ignored) {}
                            }
                            exps.add(exp);
                        }
                    }
                    experienceRepository.saveAll(exps);
                }
            }

            log.info("CV parsed and profile auto-populated for user {}", userId);
        } catch (Exception e) {
            log.warn("CV auto-parse failed for user {}: {}", userId, e.getMessage());
        }
    }

    @Transactional
    public void setDefaultCv(UUID userId, UUID cvId) {
        List<CvDocument> cvs = cvDocumentRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
        for (CvDocument cv : cvs) {
            cv.setIsDefault(cv.getId().equals(cvId));
        }
        cvDocumentRepository.saveAll(cvs);
    }

    @Transactional
    public void deleteCv(UUID userId, UUID cvId) {
        CvDocument cv = cvDocumentRepository.findByIdAndUserId(cvId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("CV not found"));
        cvDocumentRepository.delete(cv);
    }

    private CvDocumentResponse toCvResponse(CvDocument cv) {
        return new CvDocumentResponse(
                cv.getId(), cv.getFileName(), cv.getFileUrl(), cv.getParsedText(),
                cv.getVersionName(), cv.getTargetRoles(), cv.getIsDefault(), cv.getCreatedAt()
        );
    }
}
