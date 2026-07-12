package com.jobagent.service;

import com.jobagent.dto.UpdatePreferencesRequest;
import com.jobagent.dto.UserPreferencesResponse;
import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.UserPreferences;
import com.jobagent.repository.UserPreferencesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreferencesServiceTest {

    @Mock
    private UserPreferencesRepository preferencesRepository;

    @InjectMocks
    private PreferencesService preferencesService;

    private UUID userId;
    private UserPreferences existingPrefs;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        com.jobagent.model.User user = new com.jobagent.model.User();
        user.setId(userId);

        existingPrefs = UserPreferences.builder()
                .id(UUID.randomUUID())
                .user(user)
                .targetRoles("Software Engineer")
                .remoteFirst(true)
                .relocationFriendly(false)
                .minimumRemoteFitScore(new BigDecimal("75.00"))
                .maxApplicationsPerDay(10)
                .approvalRequired(true)
                .build();
    }

    @Test
    void getPreferences_whenExists_returnsResponse() {
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.of(existingPrefs));

        UserPreferencesResponse response = preferencesService.getPreferences(userId);

        assertThat(response).isNotNull();
        assertThat(response.targetRoles()).isEqualTo("Software Engineer");
        assertThat(response.remoteFirst()).isTrue();
        assertThat(response.relocationFriendly()).isFalse();
    }

    @Test
    void getPreferences_whenNotExists_createsDefault() {
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferencesRepository.save(any(UserPreferences.class))).thenAnswer(inv -> {
            UserPreferences prefs = inv.getArgument(0);
            prefs.setId(UUID.randomUUID());
            return prefs;
        });

        UserPreferencesResponse response = preferencesService.getPreferences(userId);

        assertThat(response).isNotNull();
        assertThat(response.remoteFirst()).isTrue();
        assertThat(response.approvalRequired()).isTrue();
    }

    @Test
    void updatePreferences_updatesAllFields() {
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.of(existingPrefs));
        when(preferencesRepository.save(any(UserPreferences.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePreferencesRequest request = new UpdatePreferencesRequest(
                "Senior Software Engineer", "Senior", "Java, Python", "Spring Boot", "React",
                false, true, "US, UK", "NG", "Acme Corp",
                new BigDecimal("100000"), new BigDecimal("80000"), new BigDecimal("5000000"),
                new BigDecimal("80.00"), new BigDecimal("75.00"), new BigDecimal("90.00"),
                5, true,
                "auto_reject_if_salary_below_50k", "junior,mid", "fintech,gaming"
        );

        UserPreferencesResponse response = preferencesService.updatePreferences(userId, request);

        assertThat(response.targetRoles()).isEqualTo("Senior Software Engineer");
        assertThat(response.remoteFirst()).isFalse();
        assertThat(response.relocationFriendly()).isTrue();
        assertThat(response.excludedJobLevels()).isEqualTo("junior,mid");
        assertThat(response.excludedIndustries()).isEqualTo("fintech,gaming");
        assertThat(response.autoRejectRules()).isEqualTo("auto_reject_if_salary_below_50k");
    }

    @Test
    void updatePreferences_partialUpdate_onlyUpdatesProvidedFields() {
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.of(existingPrefs));
        when(preferencesRepository.save(any(UserPreferences.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePreferencesRequest request = new UpdatePreferencesRequest(
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null, null,
                null, null,
                null, null, null
        );

        UserPreferencesResponse response = preferencesService.updatePreferences(userId, request);

        assertThat(response.targetRoles()).isEqualTo("Software Engineer");
        assertThat(response.remoteFirst()).isTrue();
    }

    @Test
    void updatePreferences_newFields_updatesCorrectly() {
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.of(existingPrefs));
        when(preferencesRepository.save(any(UserPreferences.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePreferencesRequest request = new UpdatePreferencesRequest(
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null, null,
                null, null,
                "reject_remote_below_60k", "intern", "healthcare"
        );

        UserPreferencesResponse response = preferencesService.updatePreferences(userId, request);

        assertThat(response.autoRejectRules()).isEqualTo("reject_remote_below_60k");
        assertThat(response.excludedJobLevels()).isEqualTo("intern");
        assertThat(response.excludedIndustries()).isEqualTo("healthcare");
    }
}