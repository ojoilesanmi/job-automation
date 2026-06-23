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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    private String generateToken(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );
        return jwtTokenProvider.generateToken(authentication);
    }
}
