package com.tulahack.misisbites.llmapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tulahack.misisbites.llmapi.config.LlmApiProperties;
import com.tulahack.misisbites.llmapi.dto.ChatCompletionRequest;
import com.tulahack.misisbites.llmapi.dto.ChatCompletionResponse;
import com.tulahack.misisbites.llmapi.dto.RecommendationRequest;
import com.tulahack.misisbites.llmapi.dto.RecommendationResponse;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.List;

@Service
public class LlmApiService {

    private final WebClient webClient;
    private final LlmApiProperties properties;
    private final ObjectMapper objectMapper;

    public LlmApiService(LlmApiProperties properties, ObjectMapper objectMapper) throws SSLException {
        this.properties = properties;
        this.objectMapper = objectMapper;
        
        // Create HttpClient that trusts all certificates
        HttpClient httpClient = createInsecureHttpClient();
        
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .build();
    }
    
    /**
     * Creates an HttpClient that trusts all SSL certificates.
     * Use this when connecting to servers with self-signed certificates.
     */
    private HttpClient createInsecureHttpClient() {
        try {
            SslContext sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            
            return HttpClient.create()
                    .secure(sslSpec -> sslSpec.sslContext(sslContext));
        } catch (SSLException e) {
            throw new RuntimeException("Failed to create insecure SSL context", e);
        }
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

    /**
     * Generate recommendation for a candidate based on team analytics.
     * Sends only the JSON data - system prompt is already configured in the agent.
     * Retries indefinitely until successful response is received.
     */
    public RecommendationResponse generateRecommendation(RecommendationRequest request) {
        try {
            String candidateJson = objectMapper.writeValueAsString(request);
            
            // Send only user message with data - system prompt is configured in the agent
            ChatCompletionRequest chatRequest = new ChatCompletionRequest(
                    properties.getModel(),
                    List.of(new ChatCompletionRequest.Message("user", candidateJson))
            );

            // Retry indefinitely with exponential backoff
            ChatCompletionResponse response = webClient.post()
                    .uri("/" + properties.getAgentId() + "/v1/chat/completions")
                    .bodyValue(chatRequest)
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(30))
                            .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                    .block();
            
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                String content = response.getChoices().get(0).getMessage().getContent();
                // Strip markdown code blocks if present
                content = stripMarkdownCodeBlocks(content);
                return objectMapper.readValue(content, RecommendationResponse.class);
            }
            
            throw new RuntimeException("Empty response from LLM API");
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to process JSON: " + e.getMessage(), e);
        }
    }
    
    /**
     * Strips markdown code blocks (```) from the content.
     * LLMs sometimes wrap JSON responses in ```json ... ``` blocks.
     */
    private String stripMarkdownCodeBlocks(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        // Remove ```json or ``` at the start and ``` at the end
        return content.replaceAll("^```(?:json)?\\s*", "")
                      .replaceAll("\\s*```$", "")
                      .trim();
    }
}
