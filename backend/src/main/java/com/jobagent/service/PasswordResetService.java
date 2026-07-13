package com.jobagent.service;

import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.User;
import com.jobagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final EmailService emailService;
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${app.password-reset.token-expiry-hours:1}")
    private long tokenExpiryHours;

    @Transactional
    public void requestReset(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = generateToken();
        if (redisTemplate == null) {
            throw new IllegalStateException("Password reset is temporarily unavailable");
        }
        String redisKey = "password_reset:" + userId;
        redisTemplate.opsForValue().set(redisKey, hashToken(token), tokenExpiryHours, TimeUnit.HOURS);

        emailService.sendPasswordResetEmail(user.getEmail(), token);

        notificationService.createNotification(userId, "password_reset", "Password Reset Request",
                "A password reset link has been sent to your email.",
                userId, "user");

        log.info("Password reset token generated for user {}", userId);
    }

    @Transactional
    public void resetPassword(UUID userId, String token, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (redisTemplate == null) {
            throw new IllegalStateException("Password reset is temporarily unavailable");
        }
        String redisKey = "password_reset:" + userId;
        String storedToken = redisTemplate.opsForValue().get(redisKey);

        if (storedToken == null || !storedToken.equals(hashToken(token))) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        redisTemplate.delete(redisKey);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        notificationService.createNotification(userId, "password_reset_complete", "Password Reset Successful",
                "Your password has been successfully reset.",
                userId, "user");

        log.info("Password reset completed for user {}", userId);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash reset token", e);
        }
    }
}
