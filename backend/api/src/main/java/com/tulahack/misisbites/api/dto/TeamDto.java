package com.tulahack.misisbites.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamDto {
    private Long id;
    private String name;
    private Integer limit;
    private Integer memberCount;
    private Integer totalCompatibilityPercent;
}
