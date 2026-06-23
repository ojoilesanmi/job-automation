package com.jobagent.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String targetRoles;

    private String targetSeniority;

    @Column(columnDefinition = "TEXT")
    private String preferredSkills;

    @Column(columnDefinition = "TEXT")
    private String mustHaveSkills;

    @Column(columnDefinition = "TEXT")
    private String niceToHaveSkills;

    @Builder.Default
    private Boolean remoteFirst = true;

    @Builder.Default
    private Boolean relocationFriendly = false;

    @Column(columnDefinition = "TEXT")
    private String preferredCountries;

    @Column(columnDefinition = "TEXT")
    private String excludedCountries;

    @Column(columnDefinition = "TEXT")
    private String excludedCompanies;

    private BigDecimal remoteMinSalary;

    private BigDecimal relocationMinSalary;

    private BigDecimal nigeriaMinSalary;

    @Builder.Default
    private BigDecimal minimumRemoteFitScore = new BigDecimal("75.00");

    @Builder.Default
    private BigDecimal minimumRelocationFitScore = new BigDecimal("70.00");

    @Builder.Default
    private BigDecimal minimumNigeriaFitScore = new BigDecimal("85.00");

    @Builder.Default
    private Integer maxApplicationsPerDay = 10;

    @Builder.Default
    private Boolean approvalRequired = true;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
