package com.jobagent.repository;

import com.jobagent.model.WorkExperience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkExperienceRepository extends JpaRepository<WorkExperience, UUID> {
    List<WorkExperience> findByUserIdOrderByStartDateDesc(UUID userId);
    void deleteByUserId(UUID userId);
}
