package com.tulahack.misisbites.api.repository;

import com.tulahack.misisbites.api.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    List<Candidate> findByRoleIn(List<String> roles);
    List<Candidate> findByRole(String role);
}
