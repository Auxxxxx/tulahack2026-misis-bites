package com.tulahack.misisbites.api.service;

import com.tulahack.misisbites.api.dto.CandidateAnalyticsDto;
import com.tulahack.misisbites.api.dto.CandidateRecommendationsDto;
import com.tulahack.misisbites.api.entity.Candidate;
import com.tulahack.misisbites.api.entity.Team;
import com.tulahack.misisbites.api.entity.TeamMember;
import com.tulahack.misisbites.api.repository.CandidateRepository;
import com.tulahack.misisbites.api.repository.TeamMemberRepository;
import com.tulahack.misisbites.api.repository.TeamRepository;
import com.tulahack.misisbites.compute.CompatibilityCalculator;
import com.tulahack.misisbites.compute.CompatibilityCalculator.DiscAverages;
import com.tulahack.misisbites.compute.CompatibilityCalculator.GerchikovAverages;
import com.tulahack.misisbites.llmapi.dto.RecommendationRequest;
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

        RecommendationRequest request = buildRecommendationRequest(team, members, candidate);
        RecommendationResponse response = llmApiService.generateRecommendation(request);

        return toCandidateRecommendationsDto(candidate, request, response);
    }

    private RecommendationRequest buildRecommendationRequest(Team team, List<TeamMember> members, Candidate candidate) {
        if (members.isEmpty()) {
            return RecommendationRequest.builder()
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

        List<CompatibilityCalculator.DiscMetrics> discMetrics = members.stream()
                .map(m -> new CompatibilityCalculator.DiscMetrics() {
                    public double getD() { return m.getDiscD(); }
                    public double getI() { return m.getDiscI(); }
                    public double getS() { return m.getDiscS(); }
                    public double getC() { return m.getDiscC(); }
                }).collect(Collectors.toList());

        List<CompatibilityCalculator.GerchikovMetrics> gerchikovMetrics = members.stream()
                .map(m -> new CompatibilityCalculator.GerchikovMetrics() {
                    public double getInstrumental() { return m.getGerchikovInstrumental(); }
                    public double getProfessional() { return m.getGerchikovProfessional(); }
                    public double getPatriotic() { return m.getGerchikovPatriotic(); }
                    public double getMaster() { return m.getGerchikovMaster(); }
                    public double getAvoiding() { return m.getGerchikovAvoiding(); }
                }).collect(Collectors.toList());

        DiscAverages discAvg = calculator.calculateTeamDiscAverages(discMetrics);
        GerchikovAverages gerchikovAvg = calculator.calculateTeamGerchikovAverages(gerchikovMetrics);

        int discCompat = calculator.calculateDiscCompatibility(
                candidate.getDiscD(), candidate.getDiscI(), candidate.getDiscS(), candidate.getDiscC(),
                discAvg.getD(), discAvg.getI(), discAvg.getS(), discAvg.getC());

        int gerchikovCompat = calculator.calculateGerchikovCompatibility(
                candidate.getGerchikovInstrumental(), candidate.getGerchikovProfessional(),
                candidate.getGerchikovPatriotic(), candidate.getGerchikovMaster(), candidate.getGerchikovAvoiding(),
                gerchikovAvg.getInstrumental(), gerchikovAvg.getProfessional(),
                gerchikovAvg.getPatriotic(), gerchikovAvg.getMaster(), gerchikovAvg.getAvoiding());

        int totalCompat = calculator.calculateTotalCompatibility(discCompat, gerchikovCompat);

        List<RecommendationRequest.TeamMemberData> teamMemberData = members.stream()
                .map(m -> RecommendationRequest.TeamMemberData.builder()
                        .role(m.getRole())
                        .totalCompatibilityPercent(calculateMemberTotalCompatibility(m, members))
                        .build())
                .collect(Collectors.toList());

        return RecommendationRequest.builder()
                .teamAnalytics(RecommendationRequest.TeamAnalytics.builder()
                        .avgDISC(RecommendationRequest.DiscAverages.builder()
                                .D(discAvg.getD()).I(discAvg.getI()).S(discAvg.getS()).C(discAvg.getC()).build())
                        .avgGerchikov(RecommendationRequest.GerchikovAverages.builder()
                                .INSTRUMENTAL(gerchikovAvg.getInstrumental())
                                .PROFESSIONAL(gerchikovAvg.getProfessional())
                                .PATRIOTIC(gerchikovAvg.getPatriotic())
                                .MASTER(gerchikovAvg.getMaster())
                                .AVOIDING(gerchikovAvg.getAvoiding()).build())
                        .compatibilityDiscPercent(discCompat)
                        .compatibilityGerchikovPercent(gerchikovCompat)
                        .totalCompatibilityPercent(totalCompat)
                        .build())
                .teamMembers(teamMemberData)
                .candidate(toCandidateData(candidate, discCompat, gerchikovCompat, totalCompat))
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

    private int calculateMemberTotalCompatibility(TeamMember member, List<TeamMember> allMembers) {
        List<TeamMember> otherMembers = allMembers.stream()
                .filter(m -> !m.getId().equals(member.getId()))
                .collect(Collectors.toList());

        if (otherMembers.isEmpty()) {
            return 100;
        }

        List<CompatibilityCalculator.DiscMetrics> discMetrics = otherMembers.stream()
                .map(m -> new CompatibilityCalculator.DiscMetrics() {
                    public double getD() { return m.getDiscD(); }
                    public double getI() { return m.getDiscI(); }
                    public double getS() { return m.getDiscS(); }
                    public double getC() { return m.getDiscC(); }
                }).collect(Collectors.toList());

        List<CompatibilityCalculator.GerchikovMetrics> gerchikovMetrics = otherMembers.stream()
                .map(m -> new CompatibilityCalculator.GerchikovMetrics() {
                    public double getInstrumental() { return m.getGerchikovInstrumental(); }
                    public double getProfessional() { return m.getGerchikovProfessional(); }
                    public double getPatriotic() { return m.getGerchikovPatriotic(); }
                    public double getMaster() { return m.getGerchikovMaster(); }
                    public double getAvoiding() { return m.getGerchikovAvoiding(); }
                }).collect(Collectors.toList());

        DiscAverages discAvg = calculator.calculateTeamDiscAverages(discMetrics);
        GerchikovAverages gerchikovAvg = calculator.calculateTeamGerchikovAverages(gerchikovMetrics);

        int discCompat = calculator.calculateDiscCompatibility(
                member.getDiscD(), member.getDiscI(), member.getDiscS(), member.getDiscC(),
                discAvg.getD(), discAvg.getI(), discAvg.getS(), discAvg.getC());

        int gerchikovCompat = calculator.calculateGerchikovCompatibility(
                member.getGerchikovInstrumental(), member.getGerchikovProfessional(),
                member.getGerchikovPatriotic(), member.getGerchikovMaster(), member.getGerchikovAvoiding(),
                gerchikovAvg.getInstrumental(), gerchikovAvg.getProfessional(),
                gerchikovAvg.getPatriotic(), gerchikovAvg.getMaster(), gerchikovAvg.getAvoiding());

        return calculator.calculateTotalCompatibility(discCompat, gerchikovCompat);
    }
}
