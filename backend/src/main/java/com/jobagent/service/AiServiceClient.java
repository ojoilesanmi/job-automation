package com.jobagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceClient {

    private final WebClient aiServiceWebClient;
    private final ObjectMapper objectMapper;

    @Value("${app.ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    public JsonNode parseCv(String fileUrl, String fileType) {
        try {
            return aiServiceWebClient.post()
                    .uri("/api/v1/ai/parse-cv")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("fileUrl", fileUrl, "fileType", fileType))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("AI service CV parse failed: {}", e.getMessage());
            return null;
        }
    }

    public JsonNode parseCvContent(byte[] fileContent, String fileType) {
        try {
            return aiServiceWebClient.post()
                    .uri("/api/v1/ai/parse-cv")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "fileContentBase64", Base64.getEncoder().encodeToString(fileContent),
                            "fileType", fileType))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("AI service CV content parse failed: {}", e.getMessage());
            return null;
        }
    }

    public JsonNode parseJob(String title, String company, String description) {
        try {
            return aiServiceWebClient.post()
                    .uri("/api/v1/ai/parse-job")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "title", title != null ? title : "",
                            "company", company != null ? company : "",
                            "description", description
                    ))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("AI service job parse failed: {}", e.getMessage());
            return null;
        }
    }

    public JsonNode analyzeFit(Map<String, Object> request) {
        try {
            return aiServiceWebClient.post()
                    .uri("/api/v1/ai/analyze-fit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("AI service fit analysis failed: {}", e.getMessage());
            return null;
        }
    }

    public JsonNode generateCoverLetter(Map<String, Object> request) {
        try {
            return aiServiceWebClient.post()
                    .uri("/api/v1/ai/generate-cover-letter")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("AI service cover letter generation failed: {}", e.getMessage());
            return null;
        }
    }

    public JsonNode checkInjection(String text) {
        try {
            return aiServiceWebClient.post()
                    .uri("/api/v1/ai/check-injection")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("text", text))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("AI service injection check failed: {}", e.getMessage());
            return null;
        }
    }

    public JsonNode checkQuality(String text) {
        try {
            return aiServiceWebClient.post()
                    .uri("/api/v1/ai/check-quality")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("text", text, "context", "cover_letter"))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("AI service quality check failed: {}", e.getMessage());
            return null;
        }
    }
}
