package com.jobagent.security;

import com.jobagent.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static AuthUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails) {
            return (AuthUserDetails) authentication.getPrincipal();
        }
        throw new IllegalStateException("No authenticated user found");
    }

    public static User getCurrentUser() {
        return getCurrentUserDetails().getUser();
    }

    public static java.util.UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
