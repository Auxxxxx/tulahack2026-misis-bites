package com.tulahack.misisbites.compute;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Calculator for team compatibility metrics using DISC and Gerchikov methodologies.
 * 
 * Implements the following formula:
 * S = 0.40 × S_disc + 0.35 × S_gerch + 0.25 × S_role
 * 
 * Where:
 * - S_disc: DISC compatibility (40%) - measures if candidate strengthens team's weakest axis
 * - S_gerch: Gerchikov motivational compatibility (35%) - measures if candidate shares team's motivation
 * - S_role: Role fit (25%) - measures if candidate profile matches the role requirements
 */
public class CompatibilityCalculator {

    // Weights for final score
    private static final double WEIGHT_DISC = 0.40;
    private static final double WEIGHT_GERCHIKOV = 0.35;
    private static final double WEIGHT_ROLE = 0.25;

    // Weights for S_disc calculation
    private static final double WEIGHT_L = 0.50;  // Lift
    private static final double WEIGHT_W = 0.35;  // Weakness contribution
    private static final double WEIGHT_P = 0.15;  // Penalty (inverted)

    // Role profiles - DISC ideal values for each role
    private static final Map<Role, double[]> ROLE_DISC_PROFILES = new HashMap<>();
    private static final Map<Role, double[]> ROLE_GERCHIKOV_PROFILES = new HashMap<>();

    static {
        // DISC profiles: [D, I, S, C]
        ROLE_DISC_PROFILES.put(Role.EXECUTIVE, new double[]{0.5, 0.4, 0.6, 0.5});
        ROLE_DISC_PROFILES.put(Role.PROJECT_MANAGER, new double[]{0.7, 0.6, 0.5, 0.5});
        ROLE_DISC_PROFILES.put(Role.ANALYST, new double[]{0.3, 0.4, 0.5, 0.8});
        ROLE_DISC_PROFILES.put(Role.DEVELOPER, new double[]{0.4, 0.3, 0.5, 0.7});
        ROLE_DISC_PROFILES.put(Role.DESIGNER, new double[]{0.4, 0.6, 0.4, 0.5});
        ROLE_DISC_PROFILES.put(Role.SALES, new double[]{0.7, 0.8, 0.3, 0.4});
        ROLE_DISC_PROFILES.put(Role.HR, new double[]{0.4, 0.7, 0.6, 0.4});
        ROLE_DISC_PROFILES.put(Role.SUPPORT, new double[]{0.3, 0.5, 0.7, 0.5});
        ROLE_DISC_PROFILES.put(Role.MARKETER, new double[]{0.5, 0.7, 0.4, 0.4});
        ROLE_DISC_PROFILES.put(Role.ACCOUNTANT, new double[]{0.4, 0.3, 0.5, 0.8});
        ROLE_DISC_PROFILES.put(Role.TEAM_LEAD, new double[]{0.7, 0.6, 0.5, 0.5});
        ROLE_DISC_PROFILES.put(Role.DEVOPS, new double[]{0.5, 0.4, 0.5, 0.7});
        ROLE_DISC_PROFILES.put(Role.QA_ENGINEER, new double[]{0.4, 0.4, 0.5, 0.8});
        ROLE_DISC_PROFILES.put(Role.PRODUCT_MANAGER, new double[]{0.7, 0.6, 0.4, 0.5});
        ROLE_DISC_PROFILES.put(Role.DATA_SCIENTIST, new double[]{0.4, 0.3, 0.5, 0.8});

        // Gerchikov profiles: [IN, PR, PA, HO, LU]
        // IN = Instrumental (money-driven)
        // PR = Professional (interesting tasks)
        // PA = Patriotic (belonging to something big)
        // HO = Master/Owner (autonomy, influence)
        // LU = Lumpenized (avoiding responsibility)
        ROLE_GERCHIKOV_PROFILES.put(Role.EXECUTIVE, new double[]{0.5, 0.4, 0.5, 0.4, 0.3});
        ROLE_GERCHIKOV_PROFILES.put(Role.PROJECT_MANAGER, new double[]{0.4, 0.5, 0.5, 0.7, 0.2});
        ROLE_GERCHIKOV_PROFILES.put(Role.ANALYST, new double[]{0.4, 0.7, 0.4, 0.4, 0.2});
        ROLE_GERCHIKOV_PROFILES.put(Role.DEVELOPER, new double[]{0.4, 0.8, 0.4, 0.5, 0.2});
        ROLE_GERCHIKOV_PROFILES.put(Role.DESIGNER, new double[]{0.4, 0.6, 0.5, 0.5, 0.2});
        ROLE_GERCHIKOV_PROFILES.put(Role.SALES, new double[]{0.8, 0.4, 0.5, 0.6, 0.2});
        ROLE_GERCHIKOV_PROFILES.put(Role.HR, new double[]{0.4, 0.5, 0.7, 0.5, 0.2});
        ROLE_GERCHIKOV_PROFILES.put(Role.SUPPORT, new double[]{0.4, 0.4, 0.6, 0.3, 0.3});
        ROLE_GERCHIKOV_PROFILES.put(Role.MARKETER, new double[]{0.5, 0.5, 0.5, 0.6, 0.2});
        ROLE_GERCHIKOV_PROFILES.put(Role.ACCOUNTANT, new double[]{0.6, 0.5, 0.4, 0.4, 0.2});
        ROLE_GERCHIKOV_PROFILES.put(Role.TEAM_LEAD, new double[]{0.4, 0.5, 0.4, 0.8, 0.2});
        ROLE_GERCHIKOV_PROFILES.put(Role.DEVOPS, new double[]{0.4, 0.6, 0.4, 0.6, 0.2});
        ROLE_GERCHIKOV_PROFILES.put(Role.QA_ENGINEER, new double[]{0.4, 0.6, 0.4, 0.4, 0.2});
        ROLE_GERCHIKOV_PROFILES.put(Role.PRODUCT_MANAGER, new double[]{0.4, 0.5, 0.5, 0.8, 0.2});
        ROLE_GERCHIKOV_PROFILES.put(Role.DATA_SCIENTIST, new double[]{0.4, 0.8, 0.3, 0.5, 0.2});
    }

    /**
     * Calculate total compatibility score using the formula:
     * S = 0.40 × S_disc + 0.35 × S_gerch + 0.25 × S_role
     * 
     * @param personD person's DISC D value (0-1)
     * @param personI person's DISC I value (0-1)
     * @param personS person's DISC S value (0-1)
     * @param personC person's DISC C value (0-1)
     * @param personInst person's Gerchikov Instrumental (0-1)
     * @param personProf person's Gerchikov Professional (0-1)
     * @param personPatr person's Gerchikov Patriotic (0-1)
     * @param personMast person's Gerchikov Master (0-1)
     * @param personAvoid person's Gerchikov Avoiding (0-1)
     * @param teamMembers list of team members' metrics
     * @param role the role the person is being considered for
     * @return total compatibility score (0-100)
     */
    public int calculateTotalCompatibility(
            double personD, double personI, double personS, double personC,
            double personInst, double personProf, double personPatr, 
            double personMast, double personAvoid,
            List<TeamMemberMetrics> teamMembers,
            Role role) {
        
        double sDisc = calculateS_disc(personD, personI, personS, personC, teamMembers);
        double sGerch = calculateS_gerch(personInst, personProf, personPatr, personMast, personAvoid, teamMembers);
        double sRole = calculateS_role(personD, personI, personS, personC, 
                                       personInst, personProf, personPatr, personMast, personAvoid, 
                                       role);
        
        double totalScore = WEIGHT_DISC * sDisc + WEIGHT_GERCHIKOV * sGerch + WEIGHT_ROLE * sRole;
        return Math.max(0, Math.min(100, (int) Math.round(totalScore * 100)));
    }

    /**
     * Calculate DISC compatibility score (S_disc).
     * 
     * S_disc = 0.50 × L + 0.35 × W + 0.15 × (1 − P)
     * 
     * Where:
     * - L (Lift): how much the candidate raises the team's weakest axis
     * - W (Weakness contribution): candidate's strength on team's weakest axis
     * - P (Penalty): penalty for creating a new weakness
     */
    public double calculateS_disc(
            double personD, double personI, double personS, double personC,
            List<TeamMemberMetrics> teamMembers) {
        
        if (teamMembers == null || teamMembers.isEmpty()) {
            // No team members - return neutral score
            return 0.5;
        }

        // Calculate current team averages
        double[] teamAvg = calculateTeamDiscAverages(teamMembers);
        
        // Find the weakest axis (minimum)
        double minD = teamAvg[0], minI = teamAvg[1], minS = teamAvg[2], minC = teamAvg[3];
        double oldMin = Math.min(Math.min(minD, minI), Math.min(minS, minC));
        
        // Calculate new averages with the candidate included
        int count = teamMembers.size();
        double newAvgD = (teamAvg[0] * count + personD) / (count + 1);
        double newAvgI = (teamAvg[1] * count + personI) / (count + 1);
        double newAvgS = (teamAvg[2] * count + personS) / (count + 1);
        double newAvgC = (teamAvg[3] * count + personC) / (count + 1);
        
        double newMin = Math.min(Math.min(newAvgD, newAvgI), Math.min(newAvgS, newAvgC));
        
        // Calculate L (Lift) - how much the candidate raised the minimum
        // Maximum possible lift is from oldMin to 1.0
        double maxPossibleLift = 1.0 - oldMin;
        double actualLift = Math.max(0, newMin - oldMin);
        double L = maxPossibleLift > 0 ? actualLift / maxPossibleLift : 1.0;
        
        // Calculate W (Weakness contribution) - candidate's value on the weakest axis
        double W;
        if (oldMin == teamAvg[0]) {
            W = personD;
        } else if (oldMin == teamAvg[1]) {
            W = personI;
        } else if (oldMin == teamAvg[2]) {
            W = personS;
        } else {
            W = personC;
        }
        
        // Calculate P (Penalty) - penalty for creating a new weakness
        // Check if the new minimum is in a different axis than the old one
        double P = 0.0;
        String oldMinAxis = getMinAxis(teamAvg[0], teamAvg[1], teamAvg[2], teamAvg[3]);
        String newMinAxis = getMinAxis(newAvgD, newAvgI, newAvgS, newAvgC);
        
        if (!oldMinAxis.equals(newMinAxis)) {
            // New weakness created in a different axis
            double newMinValue;
            switch (newMinAxis) {
                case "D": newMinValue = newAvgD; break;
                case "I": newMinValue = newAvgI; break;
                case "S": newMinValue = newAvgS; break;
                case "C": newMinValue = newAvgC; break;
                default: newMinValue = newMin;
            }
            // Penalty is how much lower the new minimum is compared to old minimum
            P = Math.max(0, oldMin - newMinValue);
        }
        
        // S_disc = 0.50 × L + 0.35 × W + 0.15 × (1 − P)
        return WEIGHT_L * L + WEIGHT_W * W + WEIGHT_P * (1.0 - P);
    }

    /**
     * Calculate Gerchikov motivational compatibility (S_gerch) using cosine similarity.
     * 
     * S_gerch = cos(g_mean, g_c) = (g_mean · g_c) / (|g_mean| × |g_c|)
     */
    public double calculateS_gerch(
            double personInst, double personProf, double personPatr, 
            double personMast, double personAvoid,
            List<TeamMemberMetrics> teamMembers) {
        
        if (teamMembers == null || teamMembers.isEmpty()) {
            // No team members - return neutral score
            return 0.5;
        }

        // Calculate team average Gerchikov profile
        double[] teamAvg = calculateTeamGerchikovAverages(teamMembers);
        
        // Person's Gerchikov vector
        double[] personVec = {personInst, personProf, personPatr, personMast, personAvoid};
        
        // Calculate cosine similarity
        return cosineSimilarity(teamAvg, personVec);
    }

    /**
     * Calculate role fit score (S_role).
     * 
     * S_role = 0.5 × cos(d_c, d*) + 0.5 × cos(g_c, g*)
     * 
     * where d* and g* are ideal profiles for the role.
     */
    public double calculateS_role(
            double personD, double personI, double personS, double personC,
            double personInst, double personProf, double personPatr, 
            double personMast, double personAvoid,
            Role role) {
        
        if (role == null) {
            return 0.5; // Neutral score if role is not specified
        }
        
        double[] idealDisc = ROLE_DISC_PROFILES.get(role);
        double[] idealGerchikov = ROLE_GERCHIKOV_PROFILES.get(role);
        
        if (idealDisc == null || idealGerchikov == null) {
            return 0.5; // Neutral score if role profile not found
        }
        
        double[] personDisc = {personD, personI, personS, personC};
        double[] personGerchikov = {personInst, personProf, personPatr, personMast, personAvoid};
        
        double discSimilarity = cosineSimilarity(idealDisc, personDisc);
        double gerchikovSimilarity = cosineSimilarity(idealGerchikov, personGerchikov);
        
        return 0.5 * discSimilarity + 0.5 * gerchikovSimilarity;
    }

    /**
     * Calculate cosine similarity between two vectors.
     * cos(a, b) = (a · b) / (|a| × |b|)
     */
    private double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);
        
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        
        return dotProduct / (normA * normB);
    }

    /**
     * Calculate average DISC values from team members.
     * Returns array [D, I, S, C]
     */
    public double[] calculateTeamDiscAverages(List<TeamMemberMetrics> members) {
        if (members == null || members.isEmpty()) {
            return new double[]{0.5, 0.5, 0.5, 0.5};
        }
        
        double sumD = 0, sumI = 0, sumS = 0, sumC = 0;
        for (TeamMemberMetrics m : members) {
            sumD += m.getD();
            sumI += m.getI();
            sumS += m.getS();
            sumC += m.getC();
        }
        
        int count = members.size();
        return new double[]{sumD / count, sumI / count, sumS / count, sumC / count};
    }

    /**
     * Calculate average Gerchikov values from team members.
     * Returns array [IN, PR, PA, HO, LU]
     */
    public double[] calculateTeamGerchikovAverages(List<TeamMemberMetrics> members) {
        if (members == null || members.isEmpty()) {
            return new double[]{0.5, 0.5, 0.5, 0.5, 0.5};
        }
        
        double sumInst = 0, sumProf = 0, sumPatr = 0, sumMast = 0, sumAvoid = 0;
        for (TeamMemberMetrics m : members) {
            sumInst += m.getInstrumental();
            sumProf += m.getProfessional();
            sumPatr += m.getPatriotic();
            sumMast += m.getMaster();
            sumAvoid += m.getAvoiding();
        }
        
        int count = members.size();
        return new double[]{sumInst / count, sumProf / count, sumPatr / count, sumMast / count, sumAvoid / count};
    }

    /**
     * Get the axis name with minimum value.
     */
    private String getMinAxis(double d, double i, double s, double c) {
        double min = Math.min(Math.min(d, i), Math.min(s, c));
        if (min == d) return "D";
        if (min == i) return "I";
        if (min == s) return "S";
        return "C";
    }

    /**
     * Interface for team member metrics.
     */
    public interface TeamMemberMetrics {
        double getD();
        double getI();
        double getS();
        double getC();
        double getInstrumental();
        double getProfessional();
        double getPatriotic();
        double getMaster();
        double getAvoiding();
    }

    /**
     * Supported roles for role-based compatibility calculation.
     */
    public enum Role {
        EXECUTIVE,
        PROJECT_MANAGER,
        ANALYST,
        DEVELOPER,
        DESIGNER,
        SALES,
        HR,
        SUPPORT,
        MARKETER,
        ACCOUNTANT,
        TEAM_LEAD,
        DEVOPS,
        QA_ENGINEER,
        PRODUCT_MANAGER,
        DATA_SCIENTIST
    }
}
