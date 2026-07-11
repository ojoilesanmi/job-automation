package com.jobagent.repository;

import com.jobagent.model.Certification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CertificationRepository extends JpaRepository<Certification, UUID> {
    List<Certification> findByUserIdOrderByIssueDateDesc(UUID userId);
}
