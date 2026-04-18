package com.tulahack.misisbites.api.controller;

import com.tulahack.misisbites.api.dto.*;
import com.tulahack.misisbites.api.service.RecommendationService;
import com.tulahack.misisbites.api.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "API для управления командами")
@CrossOrigin(origins = "*")
public class TeamController {

    private final TeamService teamService;
    private final RecommendationService recommendationService;

    @GetMapping
    @Operation(summary = "Получить список всех команд")
    @ApiResponse(responseCode = "200", description = "Список команд",
            content = @Content(schema = @Schema(implementation = TeamDto.class)))
    public ResponseEntity<List<TeamDto>> getTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    @GetMapping("/{id}/analytics")
    @Operation(summary = "Получить аналитику команды по ID")
    @ApiResponse(responseCode = "200", description = "Аналитика команды",
            content = @Content(schema = @Schema(implementation = TeamAnalyticsDto.class)))
    @ApiResponse(responseCode = "404", description = "Команда не найдена")
    public ResponseEntity<TeamAnalyticsDto> getTeamAnalytics(
            @Parameter(description = "ID команды") @PathVariable Long id) {
        return ResponseEntity.ok(teamService.getTeamAnalytics(id));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "Получить всех участников команды по ID")
    @ApiResponse(responseCode = "200", description = "Список участников",
            content = @Content(schema = @Schema(implementation = TeamMemberDto.class)))
    @ApiResponse(responseCode = "404", description = "Команда не найдена")
    public ResponseEntity<List<TeamMemberDto>> getTeamMembers(
            @Parameter(description = "ID команды") @PathVariable Long id) {
        return ResponseEntity.ok(teamService.getTeamMembers(id));
    }

    @GetMapping("/{teamId}/members/{memberId}/analytics")
    @Operation(summary = "Получить аналитику участника команды")
    @ApiResponse(responseCode = "200", description = "Аналитика участника",
            content = @Content(schema = @Schema(implementation = MemberAnalyticsDto.class)))
    @ApiResponse(responseCode = "404", description = "Участник или команда не найдены")
    public ResponseEntity<MemberAnalyticsDto> getMemberAnalytics(
            @Parameter(description = "ID команды") @PathVariable Long teamId,
            @Parameter(description = "ID участника") @PathVariable Long memberId) {
        return ResponseEntity.ok(teamService.getMemberAnalytics(teamId, memberId));
    }

    @GetMapping("/{id}/open-roles")
    @Operation(summary = "Получить список искомых ролей для команды")
    @ApiResponse(responseCode = "200", description = "Список искомых ролей")
    @ApiResponse(responseCode = "404", description = "Команда не найдена")
    public ResponseEntity<List<String>> getTeamOpenRoles(
            @Parameter(description = "ID команды") @PathVariable Long id) {
        return ResponseEntity.ok(teamService.getTeamOpenRoles(id));
    }

    @GetMapping("/{id}/candidates")
    @Operation(summary = "Найти кандидатов для команды по ID")
    @ApiResponse(responseCode = "200", description = "Список кандидатов с аналитикой",
            content = @Content(schema = @Schema(implementation = CandidateDto.class)))
    @ApiResponse(responseCode = "404", description = "Команда не найдена")
    @ApiResponse(responseCode = "400", description = "Некорректный ID")
    public ResponseEntity<List<CandidateDto>> getCandidatesForTeam(
            @Parameter(description = "ID команды") @PathVariable Long id,
            @Parameter(description = "Фильтр по роли") @RequestParam(required = false) String role) {
        return ResponseEntity.ok(teamService.getCandidatesForTeam(id, role));
    }

    @GetMapping("/{teamId}/candidates/{candidateId}/recommendations")
    @Operation(summary = "Получить рекомендации по кандидату")
    @ApiResponse(responseCode = "200", description = "Полные рекомендации по кандидату",
            content = @Content(schema = @Schema(implementation = CandidateRecommendationsDto.class)))
    @ApiResponse(responseCode = "404", description = "Кандидат или команда не найдены")
    @ApiResponse(responseCode = "400", description = "Некорректный ID")
    public ResponseEntity<CandidateRecommendationsDto> getCandidateRecommendations(
            @Parameter(description = "ID команды") @PathVariable Long teamId,
            @Parameter(description = "ID кандидата") @PathVariable Long candidateId) {
        return ResponseEntity.ok(recommendationService.getCandidateRecommendations(teamId, candidateId));
    }
}
