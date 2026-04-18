package com.tulahack.misisbites.llmapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tulahack.misisbites.llmapi.LlmApiTestApplication;
import com.tulahack.misisbites.llmapi.config.LlmApiProperties;
import com.tulahack.misisbites.llmapi.dto.RecommendationRequest;
import com.tulahack.misisbites.llmapi.dto.RecommendationResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = LlmApiTestApplication.class)
@ActiveProfiles("test")
class LlmApiServiceIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(LlmApiServiceIntegrationTest.class);
    @Autowired
    private LlmApiService llmApiService;

    @Autowired
    private LlmApiProperties properties;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void generateRecommendation_shouldReturnValidResponse() {
        // Arrange: Create a realistic recommendation request
        RecommendationRequest request = RecommendationRequest.builder()
                .teamAnalytics(RecommendationRequest.TeamAnalytics.builder()
                        .avgDISC(RecommendationRequest.DiscAverages.builder()
                                .D(0.65)
                                .I(0.45)
                                .S(0.50)
                                .C(0.70)
                                .build())
                        .avgGerchikov(RecommendationRequest.GerchikovAverages.builder()
                                .INSTRUMENTAL(0.60)
                                .PROFESSIONAL(0.75)
                                .PATRIOTIC(0.40)
                                .MASTER(0.68)
                                .AVOIDING(0.25)
                                .build())
                        .compatibilityDiscPercent(78)
                        .compatibilityGerchikovPercent(82)
                        .totalCompatibilityPercent(80)
                        .build())
                .teamMembers(List.of(
                        RecommendationRequest.TeamMemberData.builder()
                                .role("Frontend Developer")
                                .totalCompatibilityPercent(86)
                                .build()
                ))
                .candidate(RecommendationRequest.CandidateData.builder()
                        .role("Backend Developer")
                        .discD(0.70)
                        .discI(0.50)
                        .discS(0.30)
                        .discC(0.10)
                        .gerchikovInstrumental(0.40)
                        .gerchikovProfessional(0.20)
                        .gerchikovPatriotic(0.60)
                        .gerchikovMaster(0.30)
                        .gerchikovAvoiding(0.80)
                        .compatibilityDiscPercent(40)
                        .compatibilityGerchikovPercent(30)
                        .totalCompatibilityPercent(35)
                        .build())
                .build();

        // Act: Call the real LLM API
        RecommendationResponse response = llmApiService.generateRecommendation(request);

        // Assert: Verify the response structure
        assertNotNull(response);
        assertNotNull(response.getRecommendation());
        assertFalse(response.getRecommendation().isBlank());
        assertNotNull(response.getPros());
        assertEquals(3, response.getPros().size());
        assertNotNull(response.getCons());
        assertEquals(3, response.getCons().size());
        log.info("Generated recommendation: {}", response);
    }
}
