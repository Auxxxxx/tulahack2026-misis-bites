package com.tulahack.misisbites.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberAnalyticsDto {
    private Double DISC_D;
    private Double DISC_I;
    private Double DISC_S;
    private Double DISC_C;
    private Double GERCHIKOV_INSTRUMENTAL;
    private Double GERCHIKOV_PROFESSIONAL;
    private Double GERCHIKOV_PATRIOTIC;
    private Double GERCHIKOV_MASTER;
    private Double GERCHIKOV_AVOIDING;
    private Integer compatibilityDiscPercent;
    private Integer compatibilityGerchikovPercent;
    private Integer totalCompatibilityPercent;
}
