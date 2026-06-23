package com.jobagent.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class ServiceLoggingAspect {

    @Pointcut("execution(* com.jobagent.service..*(..))")
    public void serviceMethods() {}

    @Around("serviceMethods()")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.debug("{}.{}() called with args={}", className, methodName, sanitizeArgs(args));

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.debug("{}.{}() completed in {}ms", className, methodName, duration);
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("{}.{}() failed in {}ms: {}", className, methodName, duration, ex.getMessage());
            throw ex;
        }
    }

    private static final java.util.Set<String> SENSITIVE_FIELDS = java.util.Set.of(
        "password", "passwordHash", "token", "secret", "authorization"
    );

    private Object sanitizeArgs(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg == null) return "null";
                    if (arg.getClass().isRecord()) {
                        return sanitizeRecord(arg);
                    }
                    String str = arg.toString();
                    if (str.length() > 200) return str.substring(0, 200) + "...";
                    return str;
                })
                .reduce((a, b) -> a + ", " + b)
                .orElse("[]");
    }

    private String sanitizeRecord(Object record) {
        StringBuilder sb = new StringBuilder(record.getClass().getSimpleName() + "[");
        java.lang.reflect.RecordComponent[] components = record.getClass().getRecordComponents();
        for (int i = 0; i < components.length; i++) {
            if (i > 0) sb.append(", ");
            String name = components[i].getName();
            try {
                Object value = components[i].getAccessor().invoke(record);
                if (SENSITIVE_FIELDS.contains(name) && value != null) {
                    sb.append(name).append("=***");
                } else {
                    String str = value == null ? "null" : value.toString();
                    if (str.length() > 100) str = str.substring(0, 100) + "...";
                    sb.append(name).append("=").append(str);
                }
            } catch (Exception e) {
                sb.append(name).append("=<error>");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
