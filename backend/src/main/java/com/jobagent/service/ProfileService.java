package com.jobagent.service;

import com.jobagent.dto.*;
import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.*;
import com.jobagent.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final CvParsingService cvParsingService;

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

        cvParsingService.parseCvAndPopulateProfile(userId, cv.getId());

        return toCvResponse(cv);
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
                cv.getId(), cv.getFileName(), "/api/v1/files/cvs/" + cv.getId(), cv.getParsedText(),
                cv.getVersionName(), cv.getTargetRoles(), cv.getIsDefault(), cv.getCreatedAt()
        );
    }
}
