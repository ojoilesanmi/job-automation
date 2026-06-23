package com.jobagent.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "jobs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"source_id", "external_job_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String externalJobId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private JobSource source;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    private String companyWebsite;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    private String location;

    private String country;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    private String currency;

    private String remoteType;

    @Builder.Default
    private Boolean relocationAvailable = false;

    @Builder.Default
    private Boolean visaSponsorshipSignal = false;

    private String seniority;

    @Column(columnDefinition = "TEXT")
    private String requiredSkills;

    @Column(columnDefinition = "TEXT")
    private String preferredSkills;

    private Integer experienceYears;

    private String employmentType;

    private String applicationUrl;

    private String atsProvider;

    private OffsetDateTime datePosted;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime dateDiscovered;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> rawPayload;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
