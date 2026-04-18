package com.tulahack.misisbites.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "candidates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String role;

    @Column(name = "disc_d", nullable = false)
    private Double discD;

    @Column(name = "disc_i", nullable = false)
    private Double discI;

    @Column(name = "disc_s", nullable = false)
    private Double discS;

    @Column(name = "disc_c", nullable = false)
    private Double discC;

    @Column(name = "gerchikov_instrumental", nullable = false)
    private Double gerchikovInstrumental;

    @Column(name = "gerchikov_professional", nullable = false)
    private Double gerchikovProfessional;

    @Column(name = "gerchikov_patriotic", nullable = false)
    private Double gerchikovPatriotic;

    @Column(name = "gerchikov_master", nullable = false)
    private Double gerchikovMaster;

    @Column(name = "gerchikov_avoiding", nullable = false)
    private Double gerchikovAvoiding;
}
