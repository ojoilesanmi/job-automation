package com.jobagent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${app.ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${app.ai-service.api-key:}")
    private String aiServiceApiKey;

    @Bean
    public WebClient aiServiceWebClient() {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(aiServiceUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024));
        if (aiServiceApiKey != null && !aiServiceApiKey.isBlank()) {
            builder.defaultHeader("X-API-Key", aiServiceApiKey);
        }
        return builder.build();
    }
}
