package com.jobagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiServiceClientTest {

    private MockWebServer mockWebServer;
    private AiServiceClient aiServiceClient;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        aiServiceClient = new AiServiceClient(webClient, new ObjectMapper());
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void parseCv_whenServiceReturns200_returnsResult() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"parsedData\": {\"name\": \"John\"}}")
                .addHeader("Content-Type", "application/json"));

        JsonNode result = aiServiceClient.parseCv("https://example.com/cv.pdf", "pdf");

        assertThat(result).isNotNull();
        assertThat(result.path("parsedData").path("name").asText()).isEqualTo("John");
    }

    @Test
    void parseCv_whenServiceFails_returnsNull() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        JsonNode result = aiServiceClient.parseCv("https://example.com/cv.pdf", "pdf");

        assertThat(result).isNull();
    }

    @Test
    void generateCoverLetter_whenServiceReturns200_returnsResult() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"content\": \"Dear Hiring Manager...\"}")
                .addHeader("Content-Type", "application/json"));

        JsonNode result = aiServiceClient.generateCoverLetter(Map.of(
                "jobTitle", "Engineer",
                "company", "TestCo"
        ));

        assertThat(result).isNotNull();
        assertThat(result.path("content").asText()).contains("Dear Hiring Manager");
    }

    @Test
    void checkInjection_whenCleanText_returnsNotFlagged() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"flagged\": false}")
                .addHeader("Content-Type", "application/json"));

        JsonNode result = aiServiceClient.checkInjection("Hello, I am a software engineer.");

        assertThat(result).isNotNull();
        assertThat(result.path("flagged").asBoolean()).isFalse();
    }
}
