package com.jobagent.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.dto.ImportJobRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Test\",\"lastName\":\"User\",\"email\":\"job-test@test.com\",\"password\":\"password123\"}"))
                .andReturn();
        String response = result.getResponse().getContentAsString();
        authToken = objectMapper.readTree(response).path("data").path("token").asText();
    }

    @Test
    void importJobFromUrl() throws Exception {
        ImportJobRequest request = new ImportJobRequest(
            "https://example.com/job/123", "Test Engineer", "Test Co",
            "Job description", "Lagos", "NG", null, null, "USD",
            "remote", false, "mid", "Java,Spring", "https://apply.example.com"
        );

        mockMvc.perform(post("/api/v1/jobs/import-url")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Test Engineer"));
    }

    @Test
    void listJobs() throws Exception {
        mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobs").isArray());
    }
}
