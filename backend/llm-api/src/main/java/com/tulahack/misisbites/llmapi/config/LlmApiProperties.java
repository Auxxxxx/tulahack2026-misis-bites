package com.tulahack.misisbites.llmapi.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Configuration
@ConfigurationProperties(prefix = "llm.api")
@Validated
public class LlmApiProperties {

    private String baseUrl = "https://agent.timeweb.cloud/api/v1/cloud-ai/agents";

    @NotBlank
    private String agentId;

    @NotBlank
    private String apiKey;

    private String model = "deepseek-chat";

    public String getChatCompletionsUrl() {
        return baseUrl + "/" + agentId + "/v1/chat/completions";
    }
}
