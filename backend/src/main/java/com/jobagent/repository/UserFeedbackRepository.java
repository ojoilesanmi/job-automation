package com.jobagent.repository;

import com.jobagent.model.UserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, UUID> {
    List<UserFeedback> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT f.feedbackType, COUNT(f) FROM UserFeedback f WHERE f.user.id = :userId GROUP BY f.feedbackType")
    List<Object[]> countByFeedbackTypeGrouped(UUID userId);

    @Query("SELECT f.feedbackType, COUNT(f) FROM UserFeedback f " +
            "JOIN f.jobMatch m JOIN m.job j " +
            "WHERE f.user.id = :userId AND j.company = :company " +
            "GROUP BY f.feedbackType")
    List<Object[]> countByFeedbackTypeAndCompany(UUID userId, String company);
}
