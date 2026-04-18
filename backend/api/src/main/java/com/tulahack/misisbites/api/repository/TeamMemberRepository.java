package com.tulahack.misisbites.api.repository;

import com.tulahack.misisbites.api.entity.Team;
import com.tulahack.misisbites.api.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByTeam(Team team);
}
