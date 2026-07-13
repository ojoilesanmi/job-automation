package com.jobagent.service;

import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.Job;
import com.jobagent.model.JobMatch;
import com.jobagent.model.ProfileSkill;
import com.jobagent.model.WorkExperience;
import com.jobagent.repository.JobMatchRepository;
import com.jobagent.repository.JobRepository;
import com.jobagent.repository.ProfileSkillRepository;
import com.jobagent.repository.WorkExperienceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CvGapAnalysisService {

    private final ProfileSkillRepository skillRepository;
    private final WorkExperienceRepository experienceRepository;
    private final JobRepository jobRepository;
    private final JobMatchRepository jobMatchRepository;

    @Transactional(readOnly = true)
    public GapAnalysisResult analyzeGaps(UUID userId, UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        List<ProfileSkill> userSkills = skillRepository.findByUserId(userId);
        List<WorkExperience> experiences = experienceRepository.findByUserIdOrderByStartDateDesc(userId);

        Set<String> userSkillNames = userSkills.stream()
                .map(s -> s.getSkillName().toLowerCase())
                .collect(Collectors.toSet());

        List<String> jobRequired = parseSkills(job.getRequiredSkills());
        List<String> jobPreferred = parseSkills(job.getPreferredSkills());

        Set<String> requiredLower = jobRequired.stream().map(String::toLowerCase).collect(Collectors.toSet());
        Set<String> preferredLower = jobPreferred.stream().map(String::toLowerCase).collect(Collectors.toSet());

        Set<String> missingRequired = new LinkedHashSet<>(requiredLower);
        missingRequired.removeAll(userSkillNames);

        Set<String> missingPreferred = new LinkedHashSet<>(preferredLower);
        missingPreferred.removeAll(userSkillNames);

        List<String> skillSuggestions = new ArrayList<>();
        for (String missing : missingRequired) {
            skillSuggestions.add("Consider learning " + missing + " - required for this role");
        }
        for (String missing : missingPreferred) {
            skillSuggestions.add("Nice to have: " + missing);
        }

        List<String> experienceGaps = new ArrayList<>();
        if (job.getExperienceYears() != null && job.getExperienceYears() > 0) {
            int totalMonths = 0;
            for (WorkExperience exp : experiences) {
                if (exp.getStartDate() != null) {
                    java.time.LocalDate end = exp.getEndDate() != null ? exp.getEndDate() : java.time.LocalDate.now();
                    totalMonths += java.time.temporal.ChronoUnit.MONTHS.between(exp.getStartDate(), end);
                }
            }
            double totalYears = totalMonths / 12.0;
            if (totalYears < job.getExperienceYears()) {
                experienceGaps.add(String.format("Need %.1f more years of experience (have %.1f, need %d)",
                        job.getExperienceYears() - totalYears, totalYears, job.getExperienceYears()));
            }
        }

        List<String> recommendations = new ArrayList<>();
        if (!missingRequired.isEmpty()) {
            recommendations.add("Focus on acquiring these required skills: " + String.join(", ", missingRequired));
        }
        if (!experienceGaps.isEmpty()) {
            recommendations.add("Consider roles with lower experience requirements or highlight transferable skills");
        }
        if (missingRequired.isEmpty() && experienceGaps.isEmpty()) {
            recommendations.add("Your profile is a strong match for this role");
        }

        return new GapAnalysisResult(
                new ArrayList<>(missingRequired),
                new ArrayList<>(missingPreferred),
                skillSuggestions,
                experienceGaps,
                recommendations
        );
    }

    private List<String> parseSkills(String skills) {
        if (skills == null || skills.isBlank()) return Collections.emptyList();
        return Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public record GapAnalysisResult(
            List<String> missingRequiredSkills,
            List<String> missingPreferredSkills,
            List<String> skillSuggestions,
            List<String> experienceGaps,
            List<String> improvementRecommendations
    ) {}
}
