package com.jobagent.repository;

import com.jobagent.model.CoverLetterTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CoverLetterTemplateRepository extends JpaRepository<CoverLetterTemplate, UUID> {
    List<CoverLetterTemplate> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
