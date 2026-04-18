package com.tulahack.misisbites.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateRecommendationsDto {
    private Double DISC_D;
    private Double DISC_I;
    private Double DISC_S;
    private Double DISC_C;
    private Double GERCHIKOV_INSTRUMENTAL;
    private Double GERCHIKOV_PROFESSIONAL;
    private Double GERCHIKOV_PATRIOTIC;
    private Double GERCHIKOV_MASTER;
    private Double GERCHIKOV_AVOIDING;
    private CandidateAnalyticsDto analytics;
    private List<String> pros;
    private List<String> cons;
    private String recommendation;
}
