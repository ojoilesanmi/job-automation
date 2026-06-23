package com.jobagent.config;

import com.jobagent.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Aspect
@Component
public class AuditLoggingAspect {

    @Pointcut("execution(* com.jobagent.service..create*(..)) || " +
              "execution(* com.jobagent.service..update*(..)) || " +
              "execution(* com.jobagent.service..delete*(..)) || " +
              "execution(* com.jobagent.service..submit*(..)) || " +
              "execution(* com.jobagent.service..approve*(..)) || " +
              "execution(* com.jobagent.service..reject*(..)) || " +
              "execution(* com.jobagent.service..generate*(..)) || " +
              "execution(* com.jobagent.service..register(..)) || " +
              "execution(* com.jobagent.service..assign*(..)) || " +
              "execution(* com.jobagent.service..remove*(..)) || " +
              "execution(* com.jobagent.service..toggle*(..))")
    public void mutationMethods() {}

    @Around("mutationMethods()")
    public Object auditMutations(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        String userId = "unknown";
        try {
            userId = SecurityUtils.getCurrentUserId().toString();
        } catch (Exception ignored) {}

        String correlationId = UUID.randomUUID().toString();
        String clientIp = "unknown";
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                clientIp = xForwardedFor != null ? xForwardedFor.split(",")[0].trim() : request.getRemoteAddr();
            }
        } catch (Exception ignored) {}

        Map<String, Object> auditData = new LinkedHashMap<>();
        auditData.put("correlationId", correlationId);
        auditData.put("userId", userId);
        auditData.put("clientIp", clientIp);
        auditData.put("class", className);
        auditData.put("method", methodName);
        auditData.put("args", sanitizeArgs(joinPoint.getArgs()));

        log.info("[AUDIT] {}.{}() by user={} from ip={}", className, methodName, userId, clientIp);

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            auditData.put("status", "SUCCESS");
            auditData.put("durationMs", duration);
            log.info("[AUDIT] {}.{}() SUCCESS in {}ms", className, methodName, duration);
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            auditData.put("status", "FAILURE");
            auditData.put("error", ex.getMessage());
            auditData.put("durationMs", duration);
            log.error("[AUDIT] {}.{}() FAILURE in {}ms: {}", className, methodName, duration, ex.getMessage());
            throw ex;
        }
    }

    private Object sanitizeArgs(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg == null) return "null";
                    String str = arg.toString();
                    if (str.length() > 100) return str.substring(0, 100) + "...";
                    return str;
                })
                .reduce((a, b) -> a + ", " + b)
                .orElse("[]");
    }
}
