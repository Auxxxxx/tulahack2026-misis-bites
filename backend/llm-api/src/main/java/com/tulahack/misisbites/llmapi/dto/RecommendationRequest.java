package com.tulahack.misisbites.llmapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request object for LLM recommendation generation.
 * Contains only non-personal data (metrics and roles).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationRequest {
    
    /**
     * Team analytics data
     */
    private TeamAnalytics teamAnalytics;
    
    /**
     * List of current team members (without personal data)
     */
    private List<TeamMemberData> teamMembers;
    
    /**
     * Candidate data with their metrics (without personal data)
     */
    private CandidateData candidate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeamAnalytics {
        private DiscAverages avgDISC;
        private GerchikovAverages avgGerchikov;
        private Integer compatibilityDiscPercent;
        private Integer compatibilityGerchikovPercent;
        private Integer totalCompatibilityPercent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DiscAverages {
        private Double D;
        private Double I;
        private Double S;
        private Double C;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GerchikovAverages {
        private Double INSTRUMENTAL;
        private Double PROFESSIONAL;
        private Double PATRIOTIC;
        private Double MASTER;
        private Double AVOIDING;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeamMemberData {
        private String role;
        private Integer totalCompatibilityPercent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CandidateData {
        private String role;
        private Double discD;
        private Double discI;
        private Double discS;
        private Double discC;
        private Double gerchikovInstrumental;
        private Double gerchikovProfessional;
        private Double gerchikovPatriotic;
        private Double gerchikovMaster;
        private Double gerchikovAvoiding;
        private Integer compatibilityDiscPercent;
        private Integer compatibilityGerchikovPercent;
        private Integer totalCompatibilityPercent;
    }
}
