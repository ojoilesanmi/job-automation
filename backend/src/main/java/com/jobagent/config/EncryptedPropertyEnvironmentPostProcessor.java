package com.jobagent.config;

import com.jobagent.security.EncryptionService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

public class EncryptedPropertyEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String DECRYPTED_PROPERTY_SOURCE = "decryptedProperties";
    private static final String ENV_KEY = "ENCRYPTION_KEY";
    private static final String PROPERTY_KEY = "app.encryption.key";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String masterKey = resolveMasterKey(environment);
        Map<String, Object> decrypted = new LinkedHashMap<>();

        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (!(propertySource instanceof EnumerablePropertySource<?> enumerable)) {
                continue;
            }

            for (String name : enumerable.getPropertyNames()) {
                Object value = enumerable.getProperty(name);
                if (value instanceof String stringValue && EncryptionService.isEncrypted(stringValue)) {
                    if (masterKey == null || masterKey.isBlank()) {
                        throw new IllegalStateException("ENCRYPTION_KEY is required to decrypt property: " + name);
                    }
                    decrypted.put(name, EncryptionService.decrypt(stringValue, masterKey));
                }
            }
        }

        if (!decrypted.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(DECRYPTED_PROPERTY_SOURCE, decrypted));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private String resolveMasterKey(ConfigurableEnvironment environment) {
        String fromEnv = environment.getProperty(ENV_KEY);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return environment.getProperty(PROPERTY_KEY);
    }
}
