package com.jobagent.service;

import com.jobagent.model.GmailToken;
import com.jobagent.model.User;
import com.jobagent.repository.GmailTokenRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
public class GmailService {

    @Value("${app.gmail.credentials-path:}")
    private String credentialsPath;

    @Value("${app.gmail.token-path:./tokens}")
    private String tokenPath;

    @Value("${app.gmail.redirect-uri:http://localhost:8080/api/v1/gmail/callback}")
    private String redirectUri;

    private final GmailTokenRepository gmailTokenRepository;
    private GoogleAuthorizationCodeFlow flow;
    private final NetHttpTransport httpTransport;

    private static final Set<String> SCOPES = Set.of(GmailScopes.GMAIL_READONLY);

    public GmailService(GmailTokenRepository gmailTokenRepository) throws GeneralSecurityException, IOException {
        this.gmailTokenRepository = gmailTokenRepository;
        this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    }

    @PostConstruct
    public void init() {
        try {
            if (credentialsPath != null && !credentialsPath.isBlank() && Files.exists(Path.of(credentialsPath))) {
                GoogleClientSecrets secrets = GoogleClientSecrets.load(
                        GsonFactory.getDefaultInstance(),
                        new InputStreamReader(new FileInputStream(credentialsPath)));

                flow = new GoogleAuthorizationCodeFlow.Builder(
                        httpTransport, GsonFactory.getDefaultInstance(), secrets, SCOPES)
                        .setDataStoreFactory(new FileDataStoreFactory(Path.of(tokenPath).toFile()))
                        .setAccessType("offline")
                        .build();
                log.info("Gmail OAuth initialized");
            } else {
                log.warn("Gmail credentials not configured. Gmail integration disabled.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Gmail OAuth: {}", e.getMessage());
        }
    }

    public String getAuthorizationUrl(String state) {
        if (flow == null) {
            throw new IllegalStateException("Gmail integration not configured");
        }
        return flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(state)
                .build();
    }

    @Transactional
    public void handleCallback(UUID userId, String authorizationCode) throws Exception {
        if (flow == null) {
            throw new IllegalStateException("Gmail integration not configured");
        }
        GoogleTokenResponse tokenResponse = flow.newTokenRequest(authorizationCode)
                .setRedirectUri(redirectUri)
                .execute();

        GmailToken token = gmailTokenRepository.findByUserId(userId)
                .orElse(GmailToken.builder()
                        .user(buildUser(userId))
                        .build());

        token.setAccessToken(tokenResponse.getAccessToken());
        token.setRefreshToken(tokenResponse.getRefreshToken());
        token.setScope(String.join(" ", SCOPES));
        token.setExpiresAt(OffsetDateTime.now().plusSeconds(
                tokenResponse.getExpiresInSeconds() != null ? tokenResponse.getExpiresInSeconds() : 3600));

        gmailTokenRepository.save(token);
        log.info("Gmail tokens saved for user {}", userId);
    }

    public List<Map<String, String>> fetchRecentEmails(UUID userId, int maxResults) throws Exception {
        GmailToken token = gmailTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Gmail not connected"));

        Gmail gmail = new Gmail.Builder(httpTransport, GsonFactory.getDefaultInstance(),
                request -> request.getHeaders().setAuthorization("Bearer " + token.getAccessToken()))
                .setApplicationName("JobAgent")
                .build();

        String query = "newer_than:7d (subject:(interview OR rejection OR offer OR assessment OR application))";
        ListMessagesResponse response = gmail.users().messages().list("me")
                .setQ(query)
                .setMaxResults((long) maxResults)
                .execute();

        List<Map<String, String>> emails = new ArrayList<>();
        if (response.getMessages() != null) {
            for (Message msg : response.getMessages()) {
                Message full = gmail.users().messages().get("me", msg.getId()).setFormat("metadata").execute();
                Map<String, String> email = new HashMap<>();
                email.put("id", full.getId());
                email.put("subject", getHeader(full, "Subject"));
                email.put("from", getHeader(full, "From"));
                email.put("date", getHeader(full, "Date"));
                email.put("snippet", full.getSnippet() != null ? full.getSnippet() : "");
                emails.add(email);
            }
        }
        return emails;
    }

    private String getHeader(Message message, String name) {
        if (message.getPayload() == null || message.getPayload().getHeaders() == null) return "";
        return message.getPayload().getHeaders().stream()
                .filter(h -> name.equalsIgnoreCase(h.getName()))
                .map(h -> h.getValue())
                .findFirst().orElse("");
    }

    private User buildUser(UUID userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }

    public boolean isConfigured() {
        return flow != null;
    }
}
