package com.jobagent.repository;

import com.jobagent.model.CoverLetter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CoverLetterRepository extends JpaRepository<CoverLetter, UUID> {
    List<CoverLetter> findByUserIdAndJobIdOrderByVersionDesc(UUID userId, UUID jobId);
    List<CoverLetter> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
