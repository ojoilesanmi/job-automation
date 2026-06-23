package com.jobagent.repository;

import com.jobagent.model.CvDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CvDocumentRepository extends JpaRepository<CvDocument, UUID> {
    List<CvDocument> findByUserIdOrderByIsDefaultDescCreatedAtDesc(UUID userId);
    Optional<CvDocument> findByIdAndUserId(UUID id, UUID userId);
    Optional<CvDocument> findByUserIdAndIsDefaultTrue(UUID userId);
}
