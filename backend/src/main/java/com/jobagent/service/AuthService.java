package com.jobagent.service;

import com.jobagent.dto.AuthResponse;
import com.jobagent.dto.LoginRequest;
import com.jobagent.dto.RegisterRequest;
import com.jobagent.exception.ResourceNotFoundException;
import com.jobagent.model.Role;
import com.jobagent.model.User;
import com.jobagent.model.UserProfile;
import com.jobagent.repository.RoleRepository;
import com.jobagent.repository.UserProfileRepository;
import com.jobagent.repository.UserRepository;
import com.jobagent.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Default role not found"));

        User user = User.builder()
                .fullName(request.firstName() + " " + request.lastName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .roles(Set.of(userRole))
                .build();
        user = userRepository.save(user);

        UserProfile profile = UserProfile.builder()
                .user(user)
                .build();
        userProfileRepository.save(profile);

        Counter.builder("jobagent.auth.registrations")
                .description("Number of user registrations")
                .register(meterRegistry)
                .increment();

        String token = generateToken(request.email(), request.password());
        List<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
        return new AuthResponse(token, user.getId().toString(), user.getEmail(), request.firstName(), request.lastName(), roleNames, jwtTokenProvider.getExpirationMs());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            String token = generateToken(request.email(), request.password());

            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

            Counter.builder("jobagent.auth.logins")
                    .description("Number of successful logins")
                    .register(meterRegistry)
                    .increment();

            String[] nameParts = user.getFullName().split(" ", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : "";
            List<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
            return new AuthResponse(token, user.getId().toString(), user.getEmail(), firstName, lastName, roleNames, jwtTokenProvider.getExpirationMs());
        } catch (BadCredentialsException ex) {
            Counter.builder("jobagent.auth.login_failures")
                    .description("Number of failed login attempts")
                    .register(meterRegistry)
                    .increment();
            throw ex;
        }
    }

    @Transactional
    public AuthResponse loginWithGoogle(String code) {
        try {
            JsonNode tokenResponse = WebClient.create("https://oauth2.googleapis.com")
                    .post()
                    .uri("/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue("code=" + code
                            + "&client_id=" + googleClientId
                            + "&grant_type=authorization_code")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (tokenResponse == null || !tokenResponse.has("id_token")) {
                throw new IllegalArgumentException("Google authentication failed");
            }

            String idToken = tokenResponse.get("id_token").asText();
            JsonNode payload = parseJwtPayload(idToken);

            String email = payload.get("email").asText();
            String name = payload.has("name") ? payload.get("name").asText() : email;
            String[] nameParts = name.split(" ", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : "";

            User user = userRepository.findByEmail(email).orElseGet(() -> {
                Role userRole = roleRepository.findByName("USER")
                        .orElseThrow(() -> new ResourceNotFoundException("Default role not found"));
                User newUser = User.builder()
                        .fullName(name)
                        .email(email)
                        .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .roles(Set.of(userRole))
                        .build();
                User saved = userRepository.save(newUser);
                UserProfile profile = UserProfile.builder().user(saved).build();
                userProfileRepository.save(profile);
                return saved;
            });

            Counter.builder("jobagent.auth.google_logins")
                    .description("Number of Google OAuth logins")
                    .register(meterRegistry)
                    .increment();

            List<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, null));
            String token = jwtTokenProvider.generateToken(auth);

            return new AuthResponse(token, user.getId().toString(), user.getEmail(), firstName, lastName, roleNames, jwtTokenProvider.getExpirationMs());
        } catch (Exception e) {
            log.error("Google OAuth login failed: {}", e.getMessage());
            throw new RuntimeException("Google authentication failed: " + e.getMessage(), e);
        }
    }

    public UUID getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email))
                .getId();
    }

    private JsonNode parseJwtPayload(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) throw new IllegalArgumentException("Invalid ID token");
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Google ID token", e);
        }
    }

    private String generateToken(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );
        return jwtTokenProvider.generateToken(authentication);
    }
}
