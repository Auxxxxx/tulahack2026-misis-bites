package com.tulahack.misisbites.llmapi.service;

import com.tulahack.misisbites.llmapi.config.LlmApiProperties;
import com.tulahack.misisbites.llmapi.dto.ChatCompletionRequest;
import com.tulahack.misisbites.llmapi.dto.ChatCompletionResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class LlmApiService {

    private final WebClient webClient;
    private final LlmApiProperties properties;

    public LlmApiService(LlmApiProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .build();
    }

    public ChatCompletionResponse sendChatCompletion(ChatCompletionRequest request) {
        return webClient.post()
                .uri("/" + properties.getAgentId() + "/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .block();
    }

    public Mono<ChatCompletionResponse> sendChatCompletionAsync(ChatCompletionRequest request) {
        return webClient.post()
                .uri("/" + properties.getAgentId() + "/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class);
    }
}
