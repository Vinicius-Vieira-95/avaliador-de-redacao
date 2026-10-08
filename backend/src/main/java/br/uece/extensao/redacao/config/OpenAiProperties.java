package br.uece.extensao.redacao.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(String apiKey, String model, String baseUrl, Duration timeout) {

    public boolean chaveConfigurada() {
        return apiKey != null && !apiKey.isBlank();
    }
}
