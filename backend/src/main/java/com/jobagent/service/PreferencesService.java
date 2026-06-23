package com.jobagent.service;

import com.jobagent.dto.UpdatePreferencesRequest;
import com.jobagent.dto.UserPreferencesResponse;
import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.UserPreferences;
import com.jobagent.repository.UserPreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PreferencesService {

    private final UserPreferencesRepository preferencesRepository;

    @Transactional(readOnly = true)
    public UserPreferencesResponse getPreferences(UUID userId) {
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
        return toResponse(prefs);
    }

    @Transactional
    public UserPreferencesResponse updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        if (request.targetRoles() != null) prefs.setTargetRoles(request.targetRoles());
        if (request.targetSeniority() != null) prefs.setTargetSeniority(request.targetSeniority());
        if (request.preferredSkills() != null) prefs.setPreferredSkills(request.preferredSkills());
        if (request.mustHaveSkills() != null) prefs.setMustHaveSkills(request.mustHaveSkills());
        if (request.niceToHaveSkills() != null) prefs.setNiceToHaveSkills(request.niceToHaveSkills());
        if (request.remoteFirst() != null) prefs.setRemoteFirst(request.remoteFirst());
        if (request.relocationFriendly() != null) prefs.setRelocationFriendly(request.relocationFriendly());
        if (request.preferredCountries() != null) prefs.setPreferredCountries(request.preferredCountries());
        if (request.excludedCountries() != null) prefs.setExcludedCountries(request.excludedCountries());
        if (request.excludedCompanies() != null) prefs.setExcludedCompanies(request.excludedCompanies());
        if (request.remoteMinSalary() != null) prefs.setRemoteMinSalary(request.remoteMinSalary());
        if (request.relocationMinSalary() != null) prefs.setRelocationMinSalary(request.relocationMinSalary());
        if (request.nigeriaMinSalary() != null) prefs.setNigeriaMinSalary(request.nigeriaMinSalary());
        if (request.minimumRemoteFitScore() != null) prefs.setMinimumRemoteFitScore(request.minimumRemoteFitScore());
        if (request.minimumRelocationFitScore() != null) prefs.setMinimumRelocationFitScore(request.minimumRelocationFitScore());
        if (request.minimumNigeriaFitScore() != null) prefs.setMinimumNigeriaFitScore(request.minimumNigeriaFitScore());
        if (request.maxApplicationsPerDay() != null) prefs.setMaxApplicationsPerDay(request.maxApplicationsPerDay());
        if (request.approvalRequired() != null) prefs.setApprovalRequired(request.approvalRequired());

        prefs = preferencesRepository.save(prefs);
        return toResponse(prefs);
    }

    private UserPreferences createDefaultPreferences(UUID userId) {
        com.jobagent.model.User user = new com.jobagent.model.User();
        user.setId(userId);
        UserPreferences prefs = UserPreferences.builder().user(user).build();
        return preferencesRepository.save(prefs);
    }

    private UserPreferencesResponse toResponse(UserPreferences prefs) {
        return new UserPreferencesResponse(
                prefs.getId(), prefs.getTargetRoles(), prefs.getTargetSeniority(),
                prefs.getPreferredSkills(), prefs.getMustHaveSkills(), prefs.getNiceToHaveSkills(),
                prefs.getRemoteFirst(), prefs.getRelocationFriendly(),
                prefs.getPreferredCountries(), prefs.getExcludedCountries(), prefs.getExcludedCompanies(),
                prefs.getRemoteMinSalary(), prefs.getRelocationMinSalary(), prefs.getNigeriaMinSalary(),
                prefs.getMinimumRemoteFitScore(), prefs.getMinimumRelocationFitScore(), prefs.getMinimumNigeriaFitScore(),
                prefs.getMaxApplicationsPerDay(), prefs.getApprovalRequired(), prefs.getUpdatedAt()
        );
    }
}
