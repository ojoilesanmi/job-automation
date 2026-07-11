package com.jobagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
public class EmbeddingService {

    @Value("${app.embeddings.enabled:false}")
    private boolean enabled;

    @Value("${OPENAI_API_KEY:}")
    private String openaiApiKey;

    @Value("${app.embeddings.model:text-embedding-3-small}")
    private String model;

    @Value("${app.embeddings.dimension:1536}")
    private int dimension;

    private final ObjectMapper objectMapper;

    public EmbeddingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return enabled && openaiApiKey != null && !openaiApiKey.isBlank();
    }

    public float[] getEmbedding(String text) {
        if (!isEnabled()) {
            return generateSimpleHash(text);
        }
        try {
            WebClient client = WebClient.builder()
                    .baseUrl("https://api.openai.com/v1")
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            String requestJson = objectMapper.writeValueAsString(Map.of(
                    "input", text.substring(0, Math.min(text.length(), 8000)),
                    "model", model
            ));

            String responseJson = client.post()
                    .uri("/embeddings")
                    .bodyValue(requestJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode embedding = root.path("data").get(0).path("embedding");

            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = (float) embedding.get(i).asDouble();
            }
            return vector;
        } catch (Exception e) {
            log.warn("Embedding API failed, using fallback: {}", e.getMessage());
            return generateSimpleHash(text);
        }
    }

    public float[] getJobEmbedding(String title, String description, String requiredSkills) {
        String combined = String.join(" ", title, description, requiredSkills);
        return getEmbedding(combined);
    }

    public float[] getProfileEmbedding(String summary, String skills, String experience) {
        String combined = String.join(" ", summary, skills, experience);
        return getEmbedding(combined);
    }

    public double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0.0;
        float dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private float[] generateSimpleHash(String text) {
        float[] vector = new float[dimension];
        String normalized = text.toLowerCase().replaceAll("[^a-z0-9\\s]", "");
        String[] words = normalized.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            int hash = words[i].hashCode();
            int idx = Math.abs(hash) % dimension;
            vector[idx] += 1.0f / (i + 1);
        }
        float norm = 0;
        for (float v : vector) norm += v * v;
        norm = (float) Math.sqrt(norm);
        if (norm > 0) for (int i = 0; i < vector.length; i++) vector[i] /= norm;
        return vector;
    }
}
