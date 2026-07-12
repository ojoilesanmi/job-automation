package com.jobagent.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.dto.RegisterRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Disabled("E2E tests require a dedicated integration environment")
class EndToEndIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken() throws Exception {
        String email = "e2e-" + UUID.randomUUID() + "@test.com";
        RegisterRequest register = new RegisterRequest("E2E", "Test", email, "password123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("token")
                .asText();
    }

    @Test
    void userCanConfigureProfilePreferencesAndViewDashboard() throws Exception {
        String authHeader = "Bearer " + registerAndGetToken();

        mockMvc.perform(get("/api/v1/profile").header("Authorization", authHeader))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"headline\":\"Senior Engineer\",\"summary\":\"Experienced developer\",\"primaryRole\":\"Backend Engineer\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/preferences")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetRoles\":\"Software Engineer,Backend Developer\",\"remoteFirst\":true,\"minimumRemoteFitScore\":75}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/dashboard/overview").header("Authorization", authHeader))
                .andExpect(status().isOk());
    }

    @Test
    void notificationsEndpointsAreAvailable() throws Exception {
        String authHeader = "Bearer " + registerAndGetToken();

        mockMvc.perform(get("/api/v1/notifications").header("Authorization", authHeader))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", authHeader))
                .andExpect(status().isOk());
    }
}
