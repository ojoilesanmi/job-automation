package com.jobagent.repository;

import com.jobagent.model.GmailToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GmailTokenRepository extends JpaRepository<GmailToken, UUID> {
    Optional<GmailToken> findByUserId(UUID userId);
}
