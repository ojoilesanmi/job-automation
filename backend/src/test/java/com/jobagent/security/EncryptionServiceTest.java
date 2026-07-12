package com.jobagent.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionServiceTest {

    private static final String MASTER_KEY = "test-master-key-that-is-long-enough-for-ci";

    @Test
    void encrypt_wrapsValueInEncMarker() {
        String encrypted = EncryptionService.encrypt("secret-value", MASTER_KEY);

        assertThat(encrypted).startsWith("ENC(").endsWith(")");
        assertThat(encrypted).doesNotContain("secret-value");
    }

    @Test
    void decrypt_returnsOriginalValue() {
        String encrypted = EncryptionService.encrypt("secret-value", MASTER_KEY);

        assertThat(EncryptionService.decrypt(encrypted, MASTER_KEY)).isEqualTo("secret-value");
    }

    @Test
    void decrypt_returnsPlainValueWhenValueIsNotEncrypted() {
        assertThat(EncryptionService.decrypt("plain", MASTER_KEY)).isEqualTo("plain");
    }

    @Test
    void encrypt_requiresStrongMasterKey() {
        assertThatThrownBy(() -> EncryptionService.encrypt("secret", "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ENCRYPTION_KEY");
    }
}
