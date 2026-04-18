package com.tulahack.misisbites.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamAnalyticsDto {
    private DiscAveragesDto avgDISC;
    private GerchikovAveragesDto avgGerchikov;
    private Integer compatibilityDiscPercent;
    private Integer compatibilityGerchikovPercent;
    private Integer totalCompatibilityPercent;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DiscAveragesDto {
        private Double D;
        private Double I;
        private Double S;
        private Double C;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GerchikovAveragesDto {
        private Double INSTRUMENTAL;
        private Double PROFESSIONAL;
        private Double PATRIOTIC;
        private Double MASTER;
        private Double AVOIDING;
    }
}
