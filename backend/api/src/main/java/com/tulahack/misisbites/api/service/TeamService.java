package com.tulahack.misisbites.api.service;

import com.tulahack.misisbites.api.dto.*;
import com.tulahack.misisbites.api.entity.Candidate;
import com.tulahack.misisbites.api.entity.Team;
import com.tulahack.misisbites.api.entity.TeamMember;
import com.tulahack.misisbites.api.repository.CandidateRepository;
import com.tulahack.misisbites.api.repository.TeamMemberRepository;
import com.tulahack.misisbites.api.repository.TeamRepository;
import com.tulahack.misisbites.compute.CompatibilityCalculator;
import com.tulahack.misisbites.compute.CompatibilityCalculator.TeamMemberMetrics;
import com.tulahack.misisbites.compute.CompatibilityCalculator.Role;
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

        // Convert team members to normalized metrics
        List<TeamMemberMetrics> teamMetrics = members.stream()
                .map(TeamMemberMetricsAdapter::new)
                .collect(Collectors.toList());

        double[] discAvg = calculator.calculateTeamDiscAverages(teamMetrics);
        double[] gerchikovAvg = calculator.calculateTeamGerchikovAverages(teamMetrics);

        // Calculate average compatibility across all member pairs
        int totalCompat = 0;
        int count = 0;

        for (int i = 0; i < members.size(); i++) {
            for (int j = i + 1; j < members.size(); j++) {
                TeamMember m1 = members.get(i);
                TeamMember m2 = members.get(j);

                int memberCompat = calculateMemberTotalCompatibility(m1, members);
                totalCompat += memberCompat;
                count++;
            }
        }

        int avgCompat = count > 0 ? totalCompat / count : 0;

        TeamAnalyticsDto dto = new TeamAnalyticsDto();
        dto.setAvgDISC(new TeamAnalyticsDto.DiscAveragesDto(discAvg[0], discAvg[1], discAvg[2], discAvg[3]));
        dto.setAvgGerchikov(new TeamAnalyticsDto.GerchikovAveragesDto(
                gerchikovAvg[0], gerchikovAvg[1], gerchikovAvg[2], gerchikovAvg[3], gerchikovAvg[4]));
        dto.setCompatibilityDiscPercent(avgCompat);
        dto.setCompatibilityGerchikovPercent(avgCompat);
        dto.setTotalCompatibilityPercent(avgCompat);

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

        // Convert other members to normalized metrics
        List<TeamMemberMetrics> teamMetrics = otherMembers.stream()
                .map(TeamMemberMetricsAdapter::new)
                .collect(Collectors.toList());

        double[] discAvg = calculator.calculateTeamDiscAverages(teamMetrics);
        double[] gerchikovAvg = calculator.calculateTeamGerchikovAverages(teamMetrics);

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
        dto.setCompatibilityDiscPercent((int) Math.round(sDisc * 100));
        dto.setCompatibilityGerchikovPercent((int) Math.round(sGerch * 100));
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

        // Convert team members to normalized metrics
        List<TeamMemberMetrics> teamMetrics = members.stream()
                .map(TeamMemberMetricsAdapter::new)
                .collect(Collectors.toList());

        double[] discAvg = calculator.calculateTeamDiscAverages(teamMetrics);
        double[] gerchikovAvg = calculator.calculateTeamGerchikovAverages(teamMetrics);

        return candidates.stream()
                .map(c -> {
                    Role candidateRole = parseRole(c.getRole());
                    int totalCompat = calculator.calculateTotalCompatibility(
                            normalizeDisc(c.getDiscD()), normalizeDisc(c.getDiscI()),
                            normalizeDisc(c.getDiscS()), normalizeDisc(c.getDiscC()),
                            normalizeGerchikov(c.getGerchikovInstrumental()),
                            normalizeGerchikov(c.getGerchikovProfessional()),
                            normalizeGerchikov(c.getGerchikovPatriotic()),
                            normalizeGerchikov(c.getGerchikovMaster()),
                            normalizeGerchikov(c.getGerchikovAvoiding()),
                            teamMetrics, candidateRole);

                    double sDisc = calculator.calculateS_disc(
                            normalizeDisc(c.getDiscD()), normalizeDisc(c.getDiscI()),
                            normalizeDisc(c.getDiscS()), normalizeDisc(c.getDiscC()),
                            teamMetrics);
                    double sGerch = calculator.calculateS_gerch(
                            normalizeGerchikov(c.getGerchikovInstrumental()),
                            normalizeGerchikov(c.getGerchikovProfessional()),
                            normalizeGerchikov(c.getGerchikovPatriotic()),
                            normalizeGerchikov(c.getGerchikovMaster()),
                            normalizeGerchikov(c.getGerchikovAvoiding()),
                            teamMetrics);

                    CandidateDto dto = new CandidateDto();
                    dto.setId(c.getId());
                    dto.setFullName(c.getFullName());
                    dto.setRole(c.getRole());
                    CandidateAnalyticsDto analytics = new CandidateAnalyticsDto();
                    analytics.setCompatibilityDiscPercent((int) Math.round(sDisc * 100));
                    analytics.setCompatibilityGerchikovPercent((int) Math.round(sGerch * 100));
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

    private int calculateTeamTotalCompatibility(List<TeamMember> members) {
        if (members.size() <= 1) {
            return members.isEmpty() ? 0 : 100;
        }

        int totalCompat = 0;
        int count = 0;

        for (TeamMember member : members) {
            totalCompat += calculateMemberTotalCompatibility(member, members);
            count++;
        }

        return count > 0 ? totalCompat / count : 0;
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
     * Normalize DISC value from 0-24 scale to 0-1 scale
     */
    private double normalizeDisc(double value) {
        return value / 24.0;
    }

    /**
     * Normalize Gerchikov value from 0-10 scale to 0-1 scale
     */
    private double normalizeGerchikov(double value) {
        return value / 10.0;
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

        private double normalizeDisc(double value) {
            return value / 24.0;
        }

        private double normalizeGerchikov(double value) {
            return value / 10.0;
        }
    }
}
