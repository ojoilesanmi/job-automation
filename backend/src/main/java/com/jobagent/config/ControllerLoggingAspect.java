package com.jobagent.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Slf4j
@Aspect
@Component
public class ControllerLoggingAspect {

    @Pointcut("execution(* com.jobagent.controller..*(..))")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return joinPoint.proceed();

        HttpServletRequest request = attrs.getRequest();
        HttpServletResponse response = attrs.getResponse();

        String correlationId = UUID.randomUUID().toString();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String clientIp = getClientIp(request);

        long startTime = System.currentTimeMillis();

        log.info("[{}] >>> {} {} from {}", correlationId, method, uri, clientIp);

        Object result;
        try {
            result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("[{}] <<< {} {} completed in {}ms [status={}]",
                    correlationId, method, uri, duration, response != null ? response.getStatus() : "unknown");
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[{}] <<< {} {} failed in {}ms [error={}]",
                    correlationId, method, uri, duration, ex.getMessage());
            throw ex;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
