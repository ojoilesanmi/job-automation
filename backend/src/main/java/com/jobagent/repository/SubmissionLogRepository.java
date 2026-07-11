package com.jobagent.repository;

import com.jobagent.model.SubmissionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SubmissionLogRepository extends JpaRepository<SubmissionLog, UUID> {
    List<SubmissionLog> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);
}
