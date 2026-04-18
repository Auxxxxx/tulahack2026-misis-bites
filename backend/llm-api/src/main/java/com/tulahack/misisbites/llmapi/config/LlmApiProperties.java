package com.tulahack.misisbites.llmapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "llm.api")
public class LlmApiProperties {

    private String baseUrl = "https://agent.timeweb.cloud/api/v1/cloud-ai/agents";
    private String agentId;
    private String apiKey;
    private String model = "deepseek-chat";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getChatCompletionsUrl() {
        return baseUrl + "/" + agentId + "/v1/chat/completions";
    }
}
