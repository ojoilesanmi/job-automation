package com.jobagent.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityTest {

    @Test
    void oauthStateTokenGenerateAndValidate() {
        UUID userId = UUID.randomUUID();
        String state = OAuthStateToken.generate(userId);
        UUID validated = OAuthStateToken.validate(state);
        assertThat(validated).isEqualTo(userId);
    }

    @Test
    void oauthStateTokenRejectsTamperedState() {
        UUID userId = UUID.randomUUID();
        String state = OAuthStateToken.generate(userId);
        String tampered = state.substring(0, state.length() - 2) + "XX";
        UUID validated = OAuthStateToken.validate(tampered);
        assertThat(validated).isNull();
    }

    @Test
    void oauthStateTokenRejectsEmpty() {
        UUID validated = OAuthStateToken.validate("");
        assertThat(validated).isNull();
    }

    @Test
    void oauthStateTokenRejectsGarbage() {
        UUID validated = OAuthStateToken.validate("not-a-valid-state");
        assertThat(validated).isNull();
    }
}
