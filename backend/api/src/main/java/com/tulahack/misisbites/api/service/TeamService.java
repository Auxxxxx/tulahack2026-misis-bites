package com.tulahack.misisbites.api.service;

import com.tulahack.misisbites.api.dto.*;
import com.tulahack.misisbites.api.entity.Candidate;
import com.tulahack.misisbites.api.entity.Team;
import com.tulahack.misisbites.api.entity.TeamMember;
import com.tulahack.misisbites.api.repository.CandidateRepository;
import com.tulahack.misisbites.api.repository.TeamMemberRepository;
import com.tulahack.misisbites.api.repository.TeamRepository;
import com.tulahack.misisbites.compute.CompatibilityCalculator;
import com.tulahack.misisbites.compute.CompatibilityCalculator.DiscAverages;
import com.tulahack.misisbites.compute.CompatibilityCalculator.GerchikovAverages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CandidateRepository candidateRepository;
    private final CompatibilityCalculator calculator = new CompatibilityCalculator();

    public List<TeamDto> getAllTeams() {
        return teamRepository.findAll().stream()
                .map(this::toTeamDto)
                .collect(Collectors.toList());
    }

    public Team getTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + id));
    }

    public TeamAnalyticsDto getTeamAnalytics(Long teamId) {
        Team team = getTeamById(teamId);
        List<TeamMember> members = teamMemberRepository.findByTeam(team);

        if (members.isEmpty()) {
            TeamAnalyticsDto empty = new TeamAnalyticsDto();
            empty.setAvgDISC(new TeamAnalyticsDto.DiscAveragesDto(0.0, 0.0, 0.0, 0.0));
            empty.setAvgGerchikov(new TeamAnalyticsDto.GerchikovAveragesDto(0.0, 0.0, 0.0, 0.0, 0.0));
            empty.setCompatibilityDiscPercent(0);
            empty.setCompatibilityGerchikovPercent(0);
            empty.setTotalCompatibilityPercent(0);
            return empty;
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

        int totalDiscCompat = 0;
        int totalGerchikovCompat = 0;
        int count = 0;

        for (int i = 0; i < members.size(); i++) {
            for (int j = i + 1; j < members.size(); j++) {
                TeamMember m1 = members.get(i);
                TeamMember m2 = members.get(j);

                int discCompat = calculator.calculateDiscCompatibility(
                        m1.getDiscD(), m1.getDiscI(), m1.getDiscS(), m1.getDiscC(),
                        m2.getDiscD(), m2.getDiscI(), m2.getDiscS(), m2.getDiscC());

                int gerchikovCompat = calculator.calculateGerchikovCompatibility(
                        m1.getGerchikovInstrumental(), m1.getGerchikovProfessional(),
                        m1.getGerchikovPatriotic(), m1.getGerchikovMaster(), m1.getGerchikovAvoiding(),
                        m2.getGerchikovInstrumental(), m2.getGerchikovProfessional(),
                        m2.getGerchikovPatriotic(), m2.getGerchikovMaster(), m2.getGerchikovAvoiding());

                totalDiscCompat += discCompat;
                totalGerchikovCompat += gerchikovCompat;
                count++;
            }
        }

        int avgDiscCompat = count > 0 ? totalDiscCompat / count : 0;
        int avgGerchikovCompat = count > 0 ? totalGerchikovCompat / count : 0;
        int totalCompat = calculator.calculateTotalCompatibility(avgDiscCompat, avgGerchikovCompat);

        TeamAnalyticsDto dto = new TeamAnalyticsDto();
        dto.setAvgDISC(new TeamAnalyticsDto.DiscAveragesDto(discAvg.getD(), discAvg.getI(), discAvg.getS(), discAvg.getC()));
        dto.setAvgGerchikov(new TeamAnalyticsDto.GerchikovAveragesDto(
                gerchikovAvg.getInstrumental(), gerchikovAvg.getProfessional(),
                gerchikovAvg.getPatriotic(), gerchikovAvg.getMaster(), gerchikovAvg.getAvoiding()));
        dto.setCompatibilityDiscPercent(avgDiscCompat);
        dto.setCompatibilityGerchikovPercent(avgGerchikovCompat);
        dto.setTotalCompatibilityPercent(totalCompat);

        return dto;
    }

    public List<TeamMemberDto> getTeamMembers(Long teamId) {
        Team team = getTeamById(teamId);
        List<TeamMember> members = teamMemberRepository.findByTeam(team);

        return members.stream()
                .map(m -> {
                    TeamMemberDto dto = new TeamMemberDto();
                    dto.setId(m.getId());
                    dto.setFullName(m.getFullName());
                    dto.setRole(m.getRole());
                    dto.setTotalCompatibilityPercent(calculateMemberTotalCompatibility(m, members));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public MemberAnalyticsDto getMemberAnalytics(Long teamId, Long memberId) {
        Team team = getTeamById(teamId);
        TeamMember member = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found with id: " + memberId));

        List<TeamMember> otherMembers = teamMemberRepository.findByTeam(team).stream()
                .filter(m -> !m.getId().equals(memberId))
                .collect(Collectors.toList());

        if (otherMembers.isEmpty()) {
            MemberAnalyticsDto dto = new MemberAnalyticsDto();
            dto.setDISC_D(member.getDiscD());
            dto.setDISC_I(member.getDiscI());
            dto.setDISC_S(member.getDiscS());
            dto.setDISC_C(member.getDiscC());
            dto.setGERCHIKOV_INSTRUMENTAL(member.getGerchikovInstrumental());
            dto.setGERCHIKOV_PROFESSIONAL(member.getGerchikovProfessional());
            dto.setGERCHIKOV_PATRIOTIC(member.getGerchikovPatriotic());
            dto.setGERCHIKOV_MASTER(member.getGerchikovMaster());
            dto.setGERCHIKOV_AVOIDING(member.getGerchikovAvoiding());
            dto.setCompatibilityDiscPercent(100);
            dto.setCompatibilityGerchikovPercent(100);
            dto.setTotalCompatibilityPercent(100);
            return dto;
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

        int totalCompat = calculator.calculateTotalCompatibility(discCompat, gerchikovCompat);

        MemberAnalyticsDto dto = new MemberAnalyticsDto();
        dto.setDISC_D(member.getDiscD());
        dto.setDISC_I(member.getDiscI());
        dto.setDISC_S(member.getDiscS());
        dto.setDISC_C(member.getDiscC());
        dto.setGERCHIKOV_INSTRUMENTAL(member.getGerchikovInstrumental());
        dto.setGERCHIKOV_PROFESSIONAL(member.getGerchikovProfessional());
        dto.setGERCHIKOV_PATRIOTIC(member.getGerchikovPatriotic());
        dto.setGERCHIKOV_MASTER(member.getGerchikovMaster());
        dto.setGERCHIKOV_AVOIDING(member.getGerchikovAvoiding());
        dto.setCompatibilityDiscPercent(discCompat);
        dto.setCompatibilityGerchikovPercent(gerchikovCompat);
        dto.setTotalCompatibilityPercent(totalCompat);

        return dto;
    }

    public List<String> getTeamOpenRoles(Long teamId) {
        Team team = getTeamById(teamId);
        return team.getOpenRoles();
    }

    public List<CandidateDto> getCandidatesForTeam(Long teamId, String role) {
        Team team = getTeamById(teamId);
        List<String> roles = (role != null && !role.isEmpty()) 
                ? List.of(role) 
                : team.getOpenRoles();

        if (roles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Candidate> candidates = candidateRepository.findByRoleIn(roles);
        List<TeamMember> members = teamMemberRepository.findByTeam(team);
        
        if (members.isEmpty()) {
            return candidates.stream()
                    .map(c -> {
                        CandidateDto dto = new CandidateDto();
                        dto.setId(c.getId());
                        dto.setFullName(c.getFullName());
                        dto.setRole(c.getRole());
                        CandidateAnalyticsDto analytics = new CandidateAnalyticsDto();
                        analytics.setCompatibilityDiscPercent(100);
                        analytics.setCompatibilityGerchikovPercent(100);
                        analytics.setTotalCompatibilityPercent(100);
                        dto.setAnalytics(analytics);
                        return dto;
                    })
                    .collect(Collectors.toList());
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

        return candidates.stream()
                .map(c -> {
                    int discCompat = calculator.calculateDiscCompatibility(
                            c.getDiscD(), c.getDiscI(), c.getDiscS(), c.getDiscC(),
                            discAvg.getD(), discAvg.getI(), discAvg.getS(), discAvg.getC());

                    int gerchikovCompat = calculator.calculateGerchikovCompatibility(
                            c.getGerchikovInstrumental(), c.getGerchikovProfessional(),
                            c.getGerchikovPatriotic(), c.getGerchikovMaster(), c.getGerchikovAvoiding(),
                            gerchikovAvg.getInstrumental(), gerchikovAvg.getProfessional(),
                            gerchikovAvg.getPatriotic(), gerchikovAvg.getMaster(), gerchikovAvg.getAvoiding());

                    int totalCompat = calculator.calculateTotalCompatibility(discCompat, gerchikovCompat);

                    CandidateDto dto = new CandidateDto();
                    dto.setId(c.getId());
                    dto.setFullName(c.getFullName());
                    dto.setRole(c.getRole());
                    CandidateAnalyticsDto analytics = new CandidateAnalyticsDto();
                    analytics.setCompatibilityDiscPercent(discCompat);
                    analytics.setCompatibilityGerchikovPercent(gerchikovCompat);
                    analytics.setTotalCompatibilityPercent(totalCompat);
                    dto.setAnalytics(analytics);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private TeamDto toTeamDto(Team team) {
        TeamDto dto = new TeamDto();
        dto.setId(team.getId());
        dto.setName(team.getName());
        dto.setLimit(team.getMemberLimit());
        dto.setMemberCount(team.getMembers().size());
        
        List<TeamMember> members = team.getMembers();
        if (members.isEmpty()) {
            dto.setTotalCompatibilityPercent(0);
        } else {
            dto.setTotalCompatibilityPercent(calculateTeamTotalCompatibility(members));
        }
        
        return dto;
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

    private int calculateTeamTotalCompatibility(List<TeamMember> members) {
        if (members.size() <= 1) {
            return members.isEmpty() ? 0 : 100;
        }

        int totalDiscCompat = 0;
        int totalGerchikovCompat = 0;
        int count = 0;

        for (int i = 0; i < members.size(); i++) {
            for (int j = i + 1; j < members.size(); j++) {
                TeamMember m1 = members.get(i);
                TeamMember m2 = members.get(j);

                int discCompat = calculator.calculateDiscCompatibility(
                        m1.getDiscD(), m1.getDiscI(), m1.getDiscS(), m1.getDiscC(),
                        m2.getDiscD(), m2.getDiscI(), m2.getDiscS(), m2.getDiscC());

                int gerchikovCompat = calculator.calculateGerchikovCompatibility(
                        m1.getGerchikovInstrumental(), m1.getGerchikovProfessional(),
                        m1.getGerchikovPatriotic(), m1.getGerchikovMaster(), m1.getGerchikovAvoiding(),
                        m2.getGerchikovInstrumental(), m2.getGerchikovProfessional(),
                        m2.getGerchikovPatriotic(), m2.getGerchikovMaster(), m2.getGerchikovAvoiding());

                totalDiscCompat += discCompat;
                totalGerchikovCompat += gerchikovCompat;
                count++;
            }
        }

        int avgDiscCompat = totalDiscCompat / count;
        int avgGerchikovCompat = totalGerchikovCompat / count;
        return calculator.calculateTotalCompatibility(avgDiscCompat, avgGerchikovCompat);
    }
}
