package com.jobagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobagent.dto.*;
import com.jobagent.exception.ForbiddenException;
import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.*;
import com.jobagent.repository.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingEngine {

    private final JobMatchRepository jobMatchRepository;
    private final JobRepository jobRepository;
    private final ProfileSkillRepository skillRepository;
    private final WorkExperienceRepository experienceRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final AiServiceClient aiServiceClient;
    private final CoverLetterService coverLetterService;
    private final ApplicationService applicationService;
    private final ProfileService profileService;
    private final MeterRegistry meterRegistry;

    @Transactional
    public JobMatchResponse scoreJob(UUID userId, UUID jobId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        User user = new User();
        user.setId(userId);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        Optional<JobMatch> existing = jobMatchRepository.findByUserIdAndJobId(userId, jobId);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        List<ProfileSkill> userSkills = skillRepository.findByUserId(userId);
        List<WorkExperience> experiences = experienceRepository.findByUserIdOrderByStartDateDesc(userId);
        UserPreferences prefs = preferencesRepository.findByUserId(userId).orElse(null);

        Map<String, BigDecimal> scores = computeScoresWithAiOrFallback(userSkills, experiences, prefs, job);

        BigDecimal fitScore = scores.get("fit");
        BigDecimal skillsScore = scores.get("skills");
        BigDecimal experienceScore = scores.get("experience");
        BigDecimal roleScore = scores.get("role");
        BigDecimal locationScore = scores.get("location");
        BigDecimal salaryScore = scores.get("salary");

        Set<String> userSkillNames = userSkills.stream()
                .map(s -> s.getSkillName().toLowerCase())
                .collect(Collectors.toSet());

        List<String> jobRequiredSkills = parseSkills(job.getRequiredSkills());
        List<String> jobPreferredSkills = parseSkills(job.getPreferredSkills());

        Set<String> matchedSkills = new HashSet<>(userSkillNames);
        Set<String> jobSkills = new HashSet<>(jobRequiredSkills.stream().map(String::toLowerCase).collect(Collectors.toSet()));
        jobSkills.addAll(jobPreferredSkills.stream().map(String::toLowerCase).collect(Collectors.toSet()));
        matchedSkills.retainAll(jobSkills);

        Set<String> missingRequired = new HashSet<>(jobRequiredSkills.stream().map(String::toLowerCase).collect(Collectors.toSet()));
        missingRequired.removeAll(userSkillNames);

        List<String> reasons = new ArrayList<>();
        if (fitScore.compareTo(new BigDecimal("75")) >= 0) reasons.add("Strong overall fit");
        if (!matchedSkills.isEmpty()) reasons.add("Matches key skills: " + String.join(", ", matchedSkills));
        if (job.getRelocationAvailable() != null && job.getRelocationAvailable()) reasons.add("Relocation available");
        if ("full_remote".equals(job.getRemoteType()) || "remote".equals(job.getRemoteType())) reasons.add("Remote-friendly role");

        List<String> skipReasons = new ArrayList<>();
        if (fitScore.compareTo(new BigDecimal("50")) < 0) skipReasons.add("Low overall fit score");
        if (!missingRequired.isEmpty()) skipReasons.add("Missing required skills: " + String.join(", ", missingRequired));

        List<String> risks = new ArrayList<>();
        if (job.getSalaryMin() == null && job.getSalaryMax() == null) risks.add("Salary not disclosed");

        JobMatch match = JobMatch.builder()
                .user(user)
                .job(job)
                .fitScore(fitScore)
                .skillsScore(skillsScore)
                .experienceScore(experienceScore)
                .roleScore(roleScore)
                .locationScore(locationScore)
                .salaryScore(salaryScore)
                .matchedSkills(String.join(", ", matchedSkills))
                .missingSkills(String.join(", ", missingRequired))
                .reasonsToApply(String.join(" | ", reasons))
                .reasonsToSkip(String.join(" | ", skipReasons))
                .riskFlags(String.join(" | ", risks))
                .status("scored")
                .build();

        match = jobMatchRepository.save(match);
        sample.stop(Timer.builder("jobagent.matches.scoring_time")
                .description("Time to score jobs")
                .register(meterRegistry));
        Counter.builder("jobagent.matches.scored")
                .description("Number of jobs scored")
                .register(meterRegistry)
                .increment();
        return toResponse(match);
    }

    private Map<String, BigDecimal> computeScoresWithAiOrFallback(List<ProfileSkill> userSkills,
                                                                    List<WorkExperience> experiences,
                                                                    UserPreferences prefs, Job job) {
        try {
            Map<String, Object> aiRequest = new LinkedHashMap<>();
            List<String> skillNames = userSkills.stream().map(ProfileSkill::getSkillName).collect(Collectors.toList());
            aiRequest.put("skills", skillNames);

            List<Map<String, String>> expList = new ArrayList<>();
            for (WorkExperience exp : experiences.stream().limit(5).toList()) {
                Map<String, String> e = new LinkedHashMap<>();
                e.put("title", exp.getTitle());
                e.put("company", exp.getCompany());
                e.put("description", exp.getDescription() != null ? exp.getDescription() : "");
                expList.add(e);
            }
            aiRequest.put("experience", expList);

            Map<String, Object> jobData = new LinkedHashMap<>();
            jobData.put("title", job.getTitle());
            jobData.put("requiredSkills", parseSkills(job.getRequiredSkills()));
            jobData.put("preferredSkills", parseSkills(job.getPreferredSkills()));
            jobData.put("experienceYears", job.getExperienceYears());
            aiRequest.put("job", jobData);

            if (prefs != null) {
                Map<String, Object> prefsData = new LinkedHashMap<>();
                prefsData.put("targetRoles", prefs.getTargetRoles());
                prefsData.put("preferredCountries", prefs.getPreferredCountries());
                prefsData.put("remoteFirst", prefs.getRemoteFirst());
                prefsData.put("minSalary", prefs.getRemoteMinSalary());
                aiRequest.put("preferences", prefsData);
            }

            JsonNode aiResult = aiServiceClient.analyzeFit(aiRequest);
            if (aiResult != null && aiResult.has("fitScore")) {
                return Map.of(
                        "fit", new BigDecimal(aiResult.get("fitScore").asText()),
                        "skills", new BigDecimal(aiResult.path("skillsScore").asText("75")),
                        "experience", new BigDecimal(aiResult.path("experienceScore").asText("75")),
                        "role", new BigDecimal(aiResult.path("roleScore").asText("70")),
                        "location", new BigDecimal(aiResult.path("locationScore").asText("70")),
                        "salary", new BigDecimal(aiResult.path("salaryScore").asText("50"))
                );
            }
        } catch (Exception e) {
            log.warn("AI fit analysis failed, falling back to rule-based scoring: {}", e.getMessage());
        }

        Set<String> userSkillNames = userSkills.stream()
                .map(s -> s.getSkillName().toLowerCase())
                .collect(Collectors.toSet());

        List<String> jobRequiredSkills = parseSkills(job.getRequiredSkills());
        List<String> jobPreferredSkills = parseSkills(job.getPreferredSkills());

        BigDecimal skillsScore = calculateSkillsScore(userSkillNames, jobRequiredSkills, jobPreferredSkills);
        BigDecimal experienceScore = calculateExperienceScore(experiences, job.getExperienceYears());
        BigDecimal roleScore = calculateRoleScore(job, prefs);
        BigDecimal locationScore = calculateLocationScore(job, prefs);
        BigDecimal salaryScore = calculateSalaryScore(job, prefs);

        BigDecimal fitScore = skillsScore.multiply(new BigDecimal("0.35"))
                .add(experienceScore.multiply(new BigDecimal("0.20")))
                .add(roleScore.multiply(new BigDecimal("0.15")))
                .add(locationScore.multiply(new BigDecimal("0.15")))
                .add(salaryScore.multiply(new BigDecimal("0.10")))
                .setScale(2, RoundingMode.HALF_UP);

        return Map.of(
                "fit", fitScore,
                "skills", skillsScore,
                "experience", experienceScore,
                "role", roleScore,
                "location", locationScore,
                "salary", salaryScore
        );
    }

    @Transactional(readOnly = true)
    public MatchListResponse getMatches(UUID userId, String status, int page, int size) {
        Page<JobMatch> matches;
        if (status != null && !status.isEmpty()) {
            matches = jobMatchRepository.findByUserIdAndStatusOrderByFitScoreDesc(userId, status,
                    PageRequest.of(page, size));
        } else {
            matches = jobMatchRepository.findByUserIdOrderByFitScoreDesc(userId, PageRequest.of(page, size));
        }

        List<JobMatchResponse> matchResponses = matches.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new MatchListResponse(matchResponses, matches.getTotalElements(), matches.getTotalPages(), matches.getNumber());
    }

    @Transactional(readOnly = true)
    public JobMatchResponse getMatch(UUID matchId) {
        JobMatch match = jobMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        return toResponse(match);
    }

    private static final Set<String> ALLOWED_MATCH_STATUSES = Set.of("scored", "approved", "rejected", "saved", "pending");

    @Transactional
    public void updateMatchStatus(UUID userId, UUID matchId, String status) {
        if (!ALLOWED_MATCH_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        JobMatch match = jobMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        assertOwnership(match, userId);
        match.setStatus(status);
        jobMatchRepository.save(match);
    }

    @Transactional
    public ApproveMatchResponse approveMatch(UUID userId, UUID matchId) {
        JobMatch match = jobMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        assertOwnership(match, userId);

        match.setStatus("approved");
        jobMatchRepository.save(match);

        JobMatchResponse matchResponse = toResponse(match);

        CoverLetterResponse coverLetter = null;
        String coverLetterError = null;
        try {
            coverLetter = coverLetterService.generateCoverLetter(userId,
                    new GenerateCoverLetterRequest(match.getJob().getId(), null, "professional"));
        } catch (Exception e) {
            coverLetterError = e.getMessage();
            log.error("Cover letter generation failed for match {}: {}", matchId, e.getMessage());
        }

        ApplicationResponse application = null;
        String applicationError = null;
        try {
            UUID defaultCvId = profileService.getDefaultCvId(userId);
            application = applicationService.createApplication(userId,
                    new CreateApplicationRequest(match.getJob().getId(), defaultCvId,
                            coverLetter != null ? coverLetter.id() : null, "approval"));
        } catch (Exception e) {
            applicationError = e.getMessage();
            log.error("Application creation failed for match {}: {}", matchId, e.getMessage());
        }

        return new ApproveMatchResponse(matchResponse, coverLetter, application, coverLetterError, applicationError);
    }

    private void assertOwnership(JobMatch match, UUID userId) {
        if (!match.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this match");
        }
    }

    private BigDecimal calculateSkillsScore(Set<String> userSkills, List<String> required, List<String> preferred) {
        if (required.isEmpty() && preferred.isEmpty()) return new BigDecimal("75.00");

        Set<String> requiredLower = required.stream().map(String::toLowerCase).collect(Collectors.toSet());
        Set<String> preferredLower = preferred.stream().map(String::toLowerCase).collect(Collectors.toSet());

        long matchedRequired = requiredLower.stream().filter(userSkills::contains).count();
        long matchedPreferred = preferredLower.stream().filter(userSkills::contains).count();

        double requiredWeight = 0.7;
        double preferredWeight = 0.3;

        double score = 0;
        if (!requiredLower.isEmpty()) score += (matchedRequired / (double) requiredLower.size()) * requiredWeight * 100;
        if (!preferredLower.isEmpty()) score += (matchedPreferred / (double) preferredLower.size()) * preferredWeight * 100;
        if (requiredLower.isEmpty() && preferredLower.isEmpty()) score = 75;

        return BigDecimal.valueOf(Math.min(100, score)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateExperienceScore(List<WorkExperience> experiences, Integer requiredYears) {
        if (requiredYears == null || requiredYears == 0) return new BigDecimal("80.00");

        int totalMonths = 0;
        for (WorkExperience exp : experiences) {
            if (exp.getStartDate() != null) {
                LocalDate end = exp.getEndDate() != null ? exp.getEndDate() : LocalDate.now();
                totalMonths += java.time.temporal.ChronoUnit.MONTHS.between(exp.getStartDate(), end);
            }
        }
        double totalYears = totalMonths / 12.0;

        if (totalYears >= requiredYears) return new BigDecimal("100.00");
        if (totalYears >= requiredYears * 0.7) return new BigDecimal("80.00");
        if (totalYears >= requiredYears * 0.5) return new BigDecimal("60.00");
        return new BigDecimal("40.00");
    }

    private BigDecimal calculateRoleScore(Job job, UserPreferences prefs) {
        if (prefs == null || prefs.getTargetRoles() == null) return new BigDecimal("70.00");
        String titleLower = job.getTitle().toLowerCase();
        String[] targetRoles = prefs.getTargetRoles().toLowerCase().split(",");
        for (String role : targetRoles) {
            if (titleLower.contains(role.trim())) return new BigDecimal("100.00");
        }
        return new BigDecimal("40.00");
    }

    private BigDecimal calculateLocationScore(Job job, UserPreferences prefs) {
        if (prefs == null) return new BigDecimal("70.00");

        if ("full_remote".equals(job.getRemoteType()) || "remote".equals(job.getRemoteType())) {
            if (Boolean.TRUE.equals(prefs.getRemoteFirst())) return new BigDecimal("100.00");
            return new BigDecimal("80.00");
        }

        if (Boolean.TRUE.equals(job.getRelocationAvailable()) && Boolean.TRUE.equals(prefs.getRelocationFriendly())) {
            return new BigDecimal("90.00");
        }

        if (prefs.getPreferredCountries() != null && job.getCountry() != null) {
            String[] countries = prefs.getPreferredCountries().toLowerCase().split(",");
            for (String country : countries) {
                if (job.getCountry().toLowerCase().contains(country.trim())) return new BigDecimal("90.00");
            }
        }

        return new BigDecimal("50.00");
    }

    private BigDecimal calculateSalaryScore(Job job, UserPreferences prefs) {
        if (job.getSalaryMin() == null && job.getSalaryMax() == null) return new BigDecimal("50.00");
        if (prefs == null) return new BigDecimal("75.00");

        BigDecimal jobMax = job.getSalaryMax() != null ? job.getSalaryMax() : job.getSalaryMin();
        if (jobMax == null) return new BigDecimal("50.00");

        BigDecimal threshold = prefs.getRemoteMinSalary();
        if (threshold == null) return new BigDecimal("80.00");

        if (jobMax.compareTo(threshold) >= 0) return new BigDecimal("100.00");
        if (jobMax.compareTo(threshold.multiply(new BigDecimal("0.8"))) >= 0) return new BigDecimal("70.00");
        return new BigDecimal("40.00");
    }

    private List<String> parseSkills(String skills) {
        if (skills == null || skills.isBlank()) return Collections.emptyList();
        return Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private JobMatchResponse toResponse(JobMatch match) {
        return new JobMatchResponse(
                match.getId(), match.getJob().getId(),
                match.getJob().getTitle(), match.getJob().getCompany(),
                match.getFitScore(), match.getSkillsScore(),
                match.getExperienceScore(), match.getRoleScore(),
                match.getLocationScore(), match.getSalaryScore(),
                match.getMatchedSkills(), match.getMissingSkills(),
                match.getReasonsToApply(), match.getReasonsToSkip(),
                match.getRiskFlags(), match.getStatus(), match.getCreatedAt()
        );
    }
}
