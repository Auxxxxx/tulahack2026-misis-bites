package com.tulahack.misisbites.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teams")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "member_limit", nullable = false)
    private Integer memberLimit;

    @ElementCollection
    @CollectionTable(name = "team_open_roles", joinColumns = @JoinColumn(name = "team_id"))
    @Column(name = "role")
    @Builder.Default
    private List<String> openRoles = new ArrayList<>();

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeamMember> members = new ArrayList<>();
}
