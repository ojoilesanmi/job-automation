package com.jobagent.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ab_test_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbTestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String experimentName;

    @Column(nullable = false)
    private String variant;

    private String outcome;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;
}
