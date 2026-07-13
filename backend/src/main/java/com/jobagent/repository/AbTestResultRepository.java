package com.jobagent.repository;

import com.jobagent.model.AbTestResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AbTestResultRepository extends JpaRepository<AbTestResult, UUID> {
    List<AbTestResult> findByUserIdAndExperimentName(UUID userId, String experimentName);
    long countByExperimentNameAndVariant(String experimentName, String variant);
}
