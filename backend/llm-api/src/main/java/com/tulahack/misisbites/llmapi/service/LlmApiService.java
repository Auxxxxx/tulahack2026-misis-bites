package com.tulahack.misisbites.llmapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tulahack.misisbites.llmapi.config.LlmApiProperties;
import com.tulahack.misisbites.llmapi.dto.ChatCompletionRequest;
import com.tulahack.misisbites.llmapi.dto.ChatCompletionResponse;
import com.tulahack.misisbites.llmapi.dto.RecommendationRequest;
import com.tulahack.misisbites.llmapi.dto.RecommendationResponse;
import io.netty.handler.ssl.SslContext;
import lombok.extern.slf4j.Slf4j;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LlmApiService {

    private final WebClient webClient;
    private final LlmApiProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, RecommendationResponse> cache;

    public LlmApiService(LlmApiProperties properties, ObjectMapper objectMapper) throws SSLException {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.cache = new ConcurrentHashMap<>();
        
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
     * Retries indefinitely on HTTP errors and parsing failures until valid response is received.
     * Uses caching to avoid duplicate API calls for the same request.
     */
    public RecommendationResponse generateRecommendation(RecommendationRequest request) {
        // Generate cache key from request
        String cacheKey = generateCacheKey(request);
        
        // Check cache first
        RecommendationResponse cachedResponse = cache.get(cacheKey);
        if (cachedResponse != null) {
            log.debug("Cache hit for request key: {}", cacheKey);
            return cachedResponse;
        }
        
        String candidateJson;
        try {
            candidateJson = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize request to JSON: " + e.getMessage(), e);
        }
        
        ChatCompletionRequest chatRequest = new ChatCompletionRequest(
                properties.getModel(),
                List.of(new ChatCompletionRequest.Message("user", candidateJson))
        );
        
        int attempt = 0;
        
        while (true) {
            try {
                // Retry HTTP requests with exponential backoff
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
                    RecommendationResponse recommendationResponse = objectMapper.readValue(content, RecommendationResponse.class);
                    // Cache the successful response
                    cache.put(cacheKey, recommendationResponse);
                    log.debug("Cached recommendation for key: {}", cacheKey);
                    return recommendationResponse;
                }
                
                throw new RuntimeException("Empty response from LLM API");
            } catch (JsonProcessingException e) {
                attempt++;
                // Wait before retry with exponential backoff
                long delayMs = Duration.ofSeconds(1).multipliedBy(attempt).toMillis();
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry wait", ie);
                }
            }
        }
    }
    
    /**
     * Generates a cache key based on the request content.
     * Uses SHA-256 hash of the serialized request to create a unique key.
     * Includes the person type (CANDIDATE/TEAM_MEMBER) in the cache key.
     */
    private String generateCacheKey(RecommendationRequest request) {
        try {
            // Include type in cache key to differentiate between candidate and member recommendations
            String json = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            // Fallback to identity-based key if hashing fails
            return String.valueOf(System.identityHashCode(request));
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
