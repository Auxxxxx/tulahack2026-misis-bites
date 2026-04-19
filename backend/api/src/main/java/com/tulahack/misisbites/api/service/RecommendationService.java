package com.tulahack.misisbites.api.service;

import com.tulahack.misisbites.api.dto.CandidateAnalyticsDto;
import com.tulahack.misisbites.api.dto.CandidateRecommendationsDto;
import com.tulahack.misisbites.api.dto.MemberAnalyticsDto;
import com.tulahack.misisbites.api.dto.MemberRecommendationsDto;
import com.tulahack.misisbites.api.entity.Candidate;
import com.tulahack.misisbites.api.entity.Team;
import com.tulahack.misisbites.api.entity.TeamMember;
import com.tulahack.misisbites.api.repository.CandidateRepository;
import com.tulahack.misisbites.api.repository.TeamMemberRepository;
import com.tulahack.misisbites.api.repository.TeamRepository;
import com.tulahack.misisbites.compute.CompatibilityCalculator;
import com.tulahack.misisbites.compute.CompatibilityCalculator.TeamMemberMetrics;
import com.tulahack.misisbites.compute.CompatibilityCalculator.Role;
import com.tulahack.misisbites.llmapi.dto.RecommendationRequest;
import com.tulahack.misisbites.llmapi.dto.RecommendationRequest.PersonType;
import com.tulahack.misisbites.llmapi.dto.RecommendationResponse;
import com.tulahack.misisbites.llmapi.service.LlmApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CandidateRepository candidateRepository;
    private final LlmApiService llmApiService;
    private final CompatibilityCalculator calculator = new CompatibilityCalculator();

    public CandidateRecommendationsDto getCandidateRecommendations(Long teamId, Long candidateId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + teamId));
        
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Candidate not found with id: " + candidateId));

        List<TeamMember> members = teamMemberRepository.findByTeam(team);

        RecommendationRequest request = buildCandidateRecommendationRequest(team, members, candidate);
        RecommendationResponse response = llmApiService.generateRecommendation(request);

        return toCandidateRecommendationsDto(candidate, request, response);
    }

    public MemberRecommendationsDto getMemberRecommendations(Long teamId, Long memberId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + teamId));
        
        TeamMember member = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Team member not found with id: " + memberId));

        List<TeamMember> otherMembers = teamMemberRepository.findByTeam(team).stream()
                .filter(m -> !m.getId().equals(memberId))
                .collect(Collectors.toList());

        RecommendationRequest request = buildMemberRecommendationRequest(team, otherMembers, member);
        RecommendationResponse response = llmApiService.generateRecommendation(request);

        return toMemberRecommendationsDto(member, request, response);
    }

    private RecommendationRequest buildCandidateRecommendationRequest(Team team, List<TeamMember> members, Candidate candidate) {
        if (members.isEmpty()) {
            return RecommendationRequest.builder()
                    .type(PersonType.CANDIDATE)
                    .teamAnalytics(RecommendationRequest.TeamAnalytics.builder()
                            .avgDISC(RecommendationRequest.DiscAverages.builder()
                                    .D(0.0).I(0.0).S(0.0).C(0.0).build())
                            .avgGerchikov(RecommendationRequest.GerchikovAverages.builder()
                                    .INSTRUMENTAL(0.0).PROFESSIONAL(0.0).PATRIOTIC(0.0)
                                    .MASTER(0.0).AVOIDING(0.0).build())
                            .compatibilityDiscPercent(0)
                            .compatibilityGerchikovPercent(0)
                            .totalCompatibilityPercent(0)
                            .build())
                    .teamMembers(List.of())
                    .candidate(toCandidateData(candidate, 100, 100, 100))
                    .build();
        }

        // Convert team members to TeamMemberMetrics for the calculator
        List<TeamMemberMetrics> teamMetrics = members.stream()
                .map(m -> new TeamMemberMetricsAdapter(m))
                .collect(Collectors.toList());

        // Calculate team averages for the request
        double[] discAvg = calculator.calculateTeamDiscAverages(teamMetrics);
        double[] gerchikovAvg = calculator.calculateTeamGerchikovAverages(teamMetrics);

        // Calculate compatibility scores using the new formula
        Role role = parseRole(candidate.getRole());
        int totalCompat = calculator.calculateTotalCompatibility(
                normalizeDisc(candidate.getDiscD()), normalizeDisc(candidate.getDiscI()), 
                normalizeDisc(candidate.getDiscS()), normalizeDisc(candidate.getDiscC()),
                normalizeGerchikov(candidate.getGerchikovInstrumental()), 
                normalizeGerchikov(candidate.getGerchikovProfessional()),
                normalizeGerchikov(candidate.getGerchikovPatriotic()), 
                normalizeGerchikov(candidate.getGerchikovMaster()), 
                normalizeGerchikov(candidate.getGerchikovAvoiding()),
                teamMetrics, role);

        // Calculate individual component scores for display
        double sDisc = calculator.calculateS_disc(
                normalizeDisc(candidate.getDiscD()), normalizeDisc(candidate.getDiscI()), 
                normalizeDisc(candidate.getDiscS()), normalizeDisc(candidate.getDiscC()),
                teamMetrics);
        double sGerch = calculator.calculateS_gerch(
                normalizeGerchikov(candidate.getGerchikovInstrumental()), 
                normalizeGerchikov(candidate.getGerchikovProfessional()),
                normalizeGerchikov(candidate.getGerchikovPatriotic()), 
                normalizeGerchikov(candidate.getGerchikovMaster()), 
                normalizeGerchikov(candidate.getGerchikovAvoiding()),
                teamMetrics);

        List<RecommendationRequest.TeamMemberData> teamMemberData = members.stream()
                .map(m -> RecommendationRequest.TeamMemberData.builder()
                        .role(m.getRole())
                        .totalCompatibilityPercent(calculateMemberTotalCompatibility(m, members))
                        .build())
                .collect(Collectors.toList());

        return RecommendationRequest.builder()
                .type(PersonType.CANDIDATE)
                .teamAnalytics(RecommendationRequest.TeamAnalytics.builder()
                        .avgDISC(RecommendationRequest.DiscAverages.builder()
                                .D(discAvg[0]).I(discAvg[1]).S(discAvg[2]).C(discAvg[3]).build())
                        .avgGerchikov(RecommendationRequest.GerchikovAverages.builder()
                                .INSTRUMENTAL(gerchikovAvg[0])
                                .PROFESSIONAL(gerchikovAvg[1])
                                .PATRIOTIC(gerchikovAvg[2])
                                .MASTER(gerchikovAvg[3])
                                .AVOIDING(gerchikovAvg[4]).build())
                        .compatibilityDiscPercent((int) Math.round(sDisc * 100))
                        .compatibilityGerchikovPercent((int) Math.round(sGerch * 100))
                        .totalCompatibilityPercent(totalCompat)
                        .build())
                .teamMembers(teamMemberData)
                .candidate(toCandidateData(candidate, (int) Math.round(sDisc * 100), (int) Math.round(sGerch * 100), totalCompat))
                .build();
    }

    private RecommendationRequest buildMemberRecommendationRequest(Team team, List<TeamMember> otherMembers, TeamMember member) {
        if (otherMembers.isEmpty()) {
            return RecommendationRequest.builder()
                    .type(PersonType.TEAM_MEMBER)
                    .teamAnalytics(RecommendationRequest.TeamAnalytics.builder()
                            .avgDISC(RecommendationRequest.DiscAverages.builder()
                                    .D(0.0).I(0.0).S(0.0).C(0.0).build())
                            .avgGerchikov(RecommendationRequest.GerchikovAverages.builder()
                                    .INSTRUMENTAL(0.0).PROFESSIONAL(0.0).PATRIOTIC(0.0)
                                    .MASTER(0.0).AVOIDING(0.0).build())
                            .compatibilityDiscPercent(0)
                            .compatibilityGerchikovPercent(0)
                            .totalCompatibilityPercent(0)
                            .build())
                    .teamMembers(List.of())
                    .candidate(toCandidateDataFromMember(member, 100, 100, 100))
                    .build();
        }

        // Convert team members to TeamMemberMetrics for the calculator
        List<TeamMemberMetrics> teamMetrics = otherMembers.stream()
                .map(m -> new TeamMemberMetricsAdapter(m))
                .collect(Collectors.toList());

        // Calculate team averages for the request
        double[] discAvg = calculator.calculateTeamDiscAverages(teamMetrics);
        double[] gerchikovAvg = calculator.calculateTeamGerchikovAverages(teamMetrics);

        // Calculate compatibility scores using the new formula
        Role role = parseRole(member.getRole());
        int totalCompat = calculator.calculateTotalCompatibility(
                normalizeDisc(member.getDiscD()), normalizeDisc(member.getDiscI()), 
                normalizeDisc(member.getDiscS()), normalizeDisc(member.getDiscC()),
                normalizeGerchikov(member.getGerchikovInstrumental()), 
                normalizeGerchikov(member.getGerchikovProfessional()),
                normalizeGerchikov(member.getGerchikovPatriotic()), 
                normalizeGerchikov(member.getGerchikovMaster()), 
                normalizeGerchikov(member.getGerchikovAvoiding()),
                teamMetrics, role);

        // Calculate individual component scores for display
        double sDisc = calculator.calculateS_disc(
                normalizeDisc(member.getDiscD()), normalizeDisc(member.getDiscI()), 
                normalizeDisc(member.getDiscS()), normalizeDisc(member.getDiscC()),
                teamMetrics);
        double sGerch = calculator.calculateS_gerch(
                normalizeGerchikov(member.getGerchikovInstrumental()), 
                normalizeGerchikov(member.getGerchikovProfessional()),
                normalizeGerchikov(member.getGerchikovPatriotic()), 
                normalizeGerchikov(member.getGerchikovMaster()), 
                normalizeGerchikov(member.getGerchikovAvoiding()),
                teamMetrics);

        List<RecommendationRequest.TeamMemberData> teamMemberData = otherMembers.stream()
                .map(m -> RecommendationRequest.TeamMemberData.builder()
                        .role(m.getRole())
                        .totalCompatibilityPercent(calculateMemberTotalCompatibility(m, otherMembers))
                        .build())
                .collect(Collectors.toList());

        return RecommendationRequest.builder()
                .type(PersonType.TEAM_MEMBER)
                .teamAnalytics(RecommendationRequest.TeamAnalytics.builder()
                        .avgDISC(RecommendationRequest.DiscAverages.builder()
                                .D(discAvg[0]).I(discAvg[1]).S(discAvg[2]).C(discAvg[3]).build())
                        .avgGerchikov(RecommendationRequest.GerchikovAverages.builder()
                                .INSTRUMENTAL(gerchikovAvg[0])
                                .PROFESSIONAL(gerchikovAvg[1])
                                .PATRIOTIC(gerchikovAvg[2])
                                .MASTER(gerchikovAvg[3])
                                .AVOIDING(gerchikovAvg[4]).build())
                        .compatibilityDiscPercent((int) Math.round(sDisc * 100))
                        .compatibilityGerchikovPercent((int) Math.round(sGerch * 100))
                        .totalCompatibilityPercent(totalCompat)
                        .build())
                .teamMembers(teamMemberData)
                .candidate(toCandidateDataFromMember(member, (int) Math.round(sDisc * 100), (int) Math.round(sGerch * 100), totalCompat))
                .build();
    }

    private RecommendationRequest.CandidateData toCandidateDataFromMember(TeamMember member,
                                                                           int discCompat,
                                                                           int gerchikovCompat,
                                                                           int totalCompat) {
        return RecommendationRequest.CandidateData.builder()
                .role(member.getRole())
                .discD(member.getDiscD())
                .discI(member.getDiscI())
                .discS(member.getDiscS())
                .discC(member.getDiscC())
                .gerchikovInstrumental(member.getGerchikovInstrumental())
                .gerchikovProfessional(member.getGerchikovProfessional())
                .gerchikovPatriotic(member.getGerchikovPatriotic())
                .gerchikovMaster(member.getGerchikovMaster())
                .gerchikovAvoiding(member.getGerchikovAvoiding())
                .compatibilityDiscPercent(discCompat)
                .compatibilityGerchikovPercent(gerchikovCompat)
                .totalCompatibilityPercent(totalCompat)
                .build();
    }

    private RecommendationRequest.CandidateData toCandidateData(Candidate candidate, 
                                                                 int discCompat, 
                                                                 int gerchikovCompat, 
                                                                 int totalCompat) {
        return RecommendationRequest.CandidateData.builder()
                .role(candidate.getRole())
                .discD(candidate.getDiscD())
                .discI(candidate.getDiscI())
                .discS(candidate.getDiscS())
                .discC(candidate.getDiscC())
                .gerchikovInstrumental(candidate.getGerchikovInstrumental())
                .gerchikovProfessional(candidate.getGerchikovProfessional())
                .gerchikovPatriotic(candidate.getGerchikovPatriotic())
                .gerchikovMaster(candidate.getGerchikovMaster())
                .gerchikovAvoiding(candidate.getGerchikovAvoiding())
                .compatibilityDiscPercent(discCompat)
                .compatibilityGerchikovPercent(gerchikovCompat)
                .totalCompatibilityPercent(totalCompat)
                .build();
    }

    private CandidateRecommendationsDto toCandidateRecommendationsDto(Candidate candidate,
                                                                       RecommendationRequest request,
                                                                       RecommendationResponse response) {
        CandidateAnalyticsDto analytics = CandidateAnalyticsDto.builder()
                .compatibilityDiscPercent(request.getCandidate().getCompatibilityDiscPercent())
                .compatibilityGerchikovPercent(request.getCandidate().getCompatibilityGerchikovPercent())
                .totalCompatibilityPercent(request.getCandidate().getTotalCompatibilityPercent())
                .build();

        return CandidateRecommendationsDto.builder()
                .DISC_D(candidate.getDiscD())
                .DISC_I(candidate.getDiscI())
                .DISC_S(candidate.getDiscS())
                .DISC_C(candidate.getDiscC())
                .GERCHIKOV_INSTRUMENTAL(candidate.getGerchikovInstrumental())
                .GERCHIKOV_PROFESSIONAL(candidate.getGerchikovProfessional())
                .GERCHIKOV_PATRIOTIC(candidate.getGerchikovPatriotic())
                .GERCHIKOV_MASTER(candidate.getGerchikovMaster())
                .GERCHIKOV_AVOIDING(candidate.getGerchikovAvoiding())
                .analytics(analytics)
                .pros(response.getPros())
                .cons(response.getCons())
                .recommendation(response.getRecommendation())
                .build();
    }

    private MemberRecommendationsDto toMemberRecommendationsDto(TeamMember member,
                                                                 RecommendationRequest request,
                                                                 RecommendationResponse response) {
        MemberAnalyticsDto analytics = MemberAnalyticsDto.builder()
                .compatibilityDiscPercent(request.getCandidate().getCompatibilityDiscPercent())
                .compatibilityGerchikovPercent(request.getCandidate().getCompatibilityGerchikovPercent())
                .totalCompatibilityPercent(request.getCandidate().getTotalCompatibilityPercent())
                .build();

        return MemberRecommendationsDto.builder()
                .DISC_D(member.getDiscD())
                .DISC_I(member.getDiscI())
                .DISC_S(member.getDiscS())
                .DISC_C(member.getDiscC())
                .GERCHIKOV_INSTRUMENTAL(member.getGerchikovInstrumental())
                .GERCHIKOV_PROFESSIONAL(member.getGerchikovProfessional())
                .GERCHIKOV_PATRIOTIC(member.getGerchikovPatriotic())
                .GERCHIKOV_MASTER(member.getGerchikovMaster())
                .GERCHIKOV_AVOIDING(member.getGerchikovAvoiding())
                .analytics(analytics)
                .pros(response.getPros())
                .cons(response.getCons())
                .recommendation(response.getRecommendation())
                .build();
    }

    private int calculateMemberTotalCompatibility(TeamMember member, List<TeamMember> allMembers) {
        List<TeamMember> otherMembers = allMembers.stream()
                .filter(m -> !m.getId().equals(member.getId()))
                .collect(Collectors.toList());

        if (otherMembers.isEmpty()) {
            return 100;
        }

        List<TeamMemberMetrics> teamMetrics = otherMembers.stream()
                .map(TeamMemberMetricsAdapter::new)
                .collect(Collectors.toList());

        Role role = parseRole(member.getRole());
        return calculator.calculateTotalCompatibility(
                normalizeDisc(member.getDiscD()), normalizeDisc(member.getDiscI()), 
                normalizeDisc(member.getDiscS()), normalizeDisc(member.getDiscC()),
                normalizeGerchikov(member.getGerchikovInstrumental()), 
                normalizeGerchikov(member.getGerchikovProfessional()),
                normalizeGerchikov(member.getGerchikovPatriotic()), 
                normalizeGerchikov(member.getGerchikovMaster()), 
                normalizeGerchikov(member.getGerchikovAvoiding()),
                teamMetrics, role);
    }

    private Role parseRole(String roleString) {
        if (roleString == null || roleString.trim().isEmpty()) {
            return null;
        }
        try {
            return Role.valueOf(roleString.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Adapter to convert TeamMember entity to TeamMemberMetrics interface
     * Normalizes values: DISC / 24, Gerchikov / 10
     */
    private static class TeamMemberMetricsAdapter implements TeamMemberMetrics {
        private final TeamMember member;

        TeamMemberMetricsAdapter(TeamMember member) {
            this.member = member;
        }

        @Override
        public double getD() { return normalizeDisc(member.getDiscD()); }

        @Override
        public double getI() { return normalizeDisc(member.getDiscI()); }

        @Override
        public double getS() { return normalizeDisc(member.getDiscS()); }

        @Override
        public double getC() { return normalizeDisc(member.getDiscC()); }

        @Override
        public double getInstrumental() { return normalizeGerchikov(member.getGerchikovInstrumental()); }

        @Override
        public double getProfessional() { return normalizeGerchikov(member.getGerchikovProfessional()); }

        @Override
        public double getPatriotic() { return normalizeGerchikov(member.getGerchikovPatriotic()); }

        @Override
        public double getMaster() { return normalizeGerchikov(member.getGerchikovMaster()); }

        @Override
        public double getAvoiding() { return normalizeGerchikov(member.getGerchikovAvoiding()); }
    }

    /**
     * Normalize DISC value from 0-24 scale to 0-1 scale
     */
    private static double normalizeDisc(double value) {
        return value / 24.0;
    }

    /**
     * Normalize Gerchikov value from 0-10 scale to 0-1 scale
     */
    private static double normalizeGerchikov(double value) {
        return value / 10.0;
    }
}
