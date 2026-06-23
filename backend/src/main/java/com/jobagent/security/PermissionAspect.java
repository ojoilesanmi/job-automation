package com.jobagent.security;

import com.jobagent.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(com.jobagent.security.RequirePermission) || @annotation(com.jobagent.security.RequireRole)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserDetails)) {
            throw new ForbiddenException("Not authenticated");
        }

        AuthUserDetails userDetails = (AuthUserDetails) authentication.getPrincipal();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        if (requireRole != null) {
            String role = requireRole.value();
            if (!userDetails.hasRole(role)) {
                throw new ForbiddenException("Required role: " + role);
            }
        }

        RequirePermission requirePermission = method.getAnnotation(RequirePermission.class);
        if (requirePermission != null) {
            String permission = requirePermission.value();
            if (!userDetails.hasPermission(permission)) {
                throw new ForbiddenException("Required permission: " + permission);
            }
        }

        return joinPoint.proceed();
    }
}
