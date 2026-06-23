package com.jobagent.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Auth (1xxx)
    AUTH_INVALID_CREDENTIALS("1001", "Invalid email or password", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_EXPIRED("1002", "Token has expired", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_INVALID("1003", "Invalid authentication token", HttpStatus.UNAUTHORIZED),
    AUTH_USER_NOT_FOUND("1004", "User account not found", HttpStatus.UNAUTHORIZED),
    AUTH_EMAIL_EXISTS("1005", "Email already registered", HttpStatus.CONFLICT),
    AUTH_INSUFFICIENT_PERMISSIONS("1006", "Insufficient permissions", HttpStatus.FORBIDDEN),

    // Profile (2xxx)
    PROFILE_NOT_FOUND("2001", "Profile not found", HttpStatus.NOT_FOUND),
    PROFILE_UPDATE_FAILED("2002", "Failed to update profile", HttpStatus.INTERNAL_SERVER_ERROR),

    // CV (3xxx)
    CV_NOT_FOUND("3001", "CV document not found", HttpStatus.NOT_FOUND),
    CV_UPLOAD_FAILED("3002", "Failed to upload CV", HttpStatus.INTERNAL_SERVER_ERROR),
    CV_PARSE_FAILED("3003", "Failed to parse CV content", HttpStatus.UNPROCESSABLE_ENTITY),
    CV_DELETE_FAILED("3004", "Cannot delete the only default CV", HttpStatus.CONFLICT),

    // Jobs (4xxx)
    JOB_NOT_FOUND("4001", "Job not found", HttpStatus.NOT_FOUND),
    JOB_IMPORT_FAILED("4002", "Failed to import job from URL", HttpStatus.BAD_REQUEST),
    JOB_ALREADY_IMPORTED("4003", "Job already imported from this source", HttpStatus.CONFLICT),
    JOB_SOURCE_NOT_FOUND("4004", "Job source not found", HttpStatus.NOT_FOUND),
    JOB_SOURCE_DISABLED("4005", "Job source is disabled", HttpStatus.BAD_REQUEST),

    // Matching (5xxx)
    MATCH_NOT_FOUND("5001", "Match not found", HttpStatus.NOT_FOUND),
    MATCH_ALREADY_EXISTS("5002", "Match already exists for this job", HttpStatus.CONFLICT),
    MATCH_INVALID_STATUS("5003", "Invalid match status transition", HttpStatus.BAD_REQUEST),
    MATCH_SCORING_FAILED("5004", "Failed to score job", HttpStatus.INTERNAL_SERVER_ERROR),

    // Cover Letters (6xxx)
    COVER_LETTER_NOT_FOUND("6001", "Cover letter not found", HttpStatus.NOT_FOUND),
    COVER_LETTER_GENERATION_FAILED("6002", "Failed to generate cover letter", HttpStatus.INTERNAL_SERVER_ERROR),
    COVER_LETTER_UPDATE_FAILED("6003", "Failed to update cover letter", HttpStatus.INTERNAL_SERVER_ERROR),

    // Applications (7xxx)
    APPLICATION_NOT_FOUND("7001", "Application not found", HttpStatus.NOT_FOUND),
    APPLICATION_DUPLICATE("7002", "Application already exists for this job", HttpStatus.CONFLICT),
    APPLICATION_INVALID_STATUS("7003", "Invalid application status transition", HttpStatus.BAD_REQUEST),
    APPLICATION_SUBMIT_FAILED("7004", "Failed to submit application", HttpStatus.INTERNAL_SERVER_ERROR),
    APPLICATION_DAILY_LIMIT("7005", "Daily application limit reached", HttpStatus.TOO_MANY_REQUESTS),

    // Preferences (8xxx)
    PREFERENCES_NOT_FOUND("8001", "Preferences not found", HttpStatus.NOT_FOUND),
    PREFERENCES_UPDATE_FAILED("8002", "Failed to update preferences", HttpStatus.INTERNAL_SERVER_ERROR),

    // Admin (9xxx)
    ADMIN_ROLE_NOT_FOUND("9001", "Role not found", HttpStatus.NOT_FOUND),
    ADMIN_ROLE_EXISTS("9002", "Role name already exists", HttpStatus.CONFLICT),
    ADMIN_CANNOT_DELETE_SYSTEM_ROLE("9003", "Cannot delete system role", HttpStatus.FORBIDDEN),
    ADMIN_USER_NOT_FOUND("9004", "User not found", HttpStatus.NOT_FOUND),
    ADMIN_PERMISSION_NOT_FOUND("9005", "Permission not found", HttpStatus.NOT_FOUND),

    // System (0xxx)
    SYSTEM_INTERNAL_ERROR("0001", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    SYSTEM_VALIDATION_ERROR("0002", "Validation failed", HttpStatus.BAD_REQUEST),
    SYSTEM_RESOURCE_UNAVAILABLE("0003", "Resource temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    SYSTEM_RATE_LIMITED("0004", "Rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
