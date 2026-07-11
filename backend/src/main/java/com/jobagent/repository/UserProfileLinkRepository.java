package com.jobagent.repository;

import com.jobagent.model.UserProfileLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserProfileLinkRepository extends JpaRepository<UserProfileLink, UUID> {
    List<UserProfileLink> findByUserId(UUID userId);
}
