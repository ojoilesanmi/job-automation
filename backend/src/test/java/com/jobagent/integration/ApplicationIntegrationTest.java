package com.jobagent.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Test\",\"lastName\":\"User\",\"email\":\"app-test@test.com\",\"password\":\"password123\"}"))
                .andReturn();
        String response = result.getResponse().getContentAsString();
        authToken = objectMapper.readTree(response).path("data").path("token").asText();
    }

    @Test
    void listApplications() throws Exception {
        mockMvc.perform(get("/api/v1/applications")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applications").isArray());
    }

    @Test
    void getApplicationNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/applications/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }
}
