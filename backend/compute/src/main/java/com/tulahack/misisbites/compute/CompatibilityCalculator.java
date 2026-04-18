package com.tulahack.misisbites.compute;

import java.util.List;

/**
 * Calculator for team compatibility metrics using DISC and Gerchikov methodologies.
 * Plain Java class - no Spring dependencies.
 */
public class CompatibilityCalculator {

    /**
     * Calculate DISC compatibility percentage between a person and a team.
     */
    public int calculateDiscCompatibility(
            double personD, double personI, double personS, double personC,
            double teamAvgD, double teamAvgI, double teamAvgS, double teamAvgC) {
        
        double diffD = Math.abs(personD - teamAvgD);
        double diffI = Math.abs(personI - teamAvgI);
        double diffS = Math.abs(personS - teamAvgS);
        double diffC = Math.abs(personC - teamAvgC);
        
        double avgDiff = (diffD + diffI + diffS + diffC) / 4.0;
        double compatibility = (1.0 - avgDiff) * 100;
        
        return Math.max(0, Math.min(100, (int) Math.round(compatibility)));
    }

    /**
     * Calculate Gerchikov compatibility percentage between a person and a team.
     */
    public int calculateGerchikovCompatibility(
            double personInst, double personProf, double personPatr, 
            double personMast, double personAvoid,
            double teamAvgInst, double teamAvgProf, double teamAvgPatr,
            double teamAvgMast, double teamAvgAvoid) {
        
        double diffInst = Math.abs(personInst - teamAvgInst);
        double diffProf = Math.abs(personProf - teamAvgProf);
        double diffPatr = Math.abs(personPatr - teamAvgPatr);
        double diffMast = Math.abs(personMast - teamAvgMast);
        double diffAvoid = Math.abs(personAvoid - teamAvgAvoid);
        
        double avgDiff = (diffInst + diffProf + diffPatr + diffMast + diffAvoid) / 5.0;
        double compatibility = (1.0 - avgDiff) * 100;
        
        return Math.max(0, Math.min(100, (int) Math.round(compatibility)));
    }

    /**
     * Calculate total compatibility as average of DISC and Gerchikov compatibility.
     */
    public int calculateTotalCompatibility(int discCompatibility, int gerchikovCompatibility) {
        return (discCompatibility + gerchikovCompatibility) / 2;
    }

    /**
     * Calculate average DISC values from a list of team members.
     */
    public DiscAverages calculateTeamDiscAverages(List<DiscMetrics> members) {
        if (members.isEmpty()) {
            return new DiscAverages(0, 0, 0, 0);
        }
        
        double sumD = 0, sumI = 0, sumS = 0, sumC = 0;
        for (DiscMetrics m : members) {
            sumD += m.getD();
            sumI += m.getI();
            sumS += m.getS();
            sumC += m.getC();
        }
        
        int count = members.size();
        return new DiscAverages(sumD / count, sumI / count, sumS / count, sumC / count);
    }

    /**
     * Calculate average Gerchikov values from a list of team members.
     */
    public GerchikovAverages calculateTeamGerchikovAverages(List<GerchikovMetrics> members) {
        if (members.isEmpty()) {
            return new GerchikovAverages(0, 0, 0, 0, 0);
        }
        
        double sumInst = 0, sumProf = 0, sumPatr = 0, sumMast = 0, sumAvoid = 0;
        for (GerchikovMetrics m : members) {
            sumInst += m.getInstrumental();
            sumProf += m.getProfessional();
            sumPatr += m.getPatriotic();
            sumMast += m.getMaster();
            sumAvoid += m.getAvoiding();
        }
        
        int count = members.size();
        return new GerchikovAverages(
            sumInst / count, sumProf / count, sumPatr / count, 
            sumMast / count, sumAvoid / count
        );
    }

    public static class DiscAverages {
        private final double D, I, S, C;
        
        public DiscAverages(double D, double I, double S, double C) {
            this.D = D;
            this.I = I;
            this.S = S;
            this.C = C;
        }
        
        public double getD() { return D; }
        public double getI() { return I; }
        public double getS() { return S; }
        public double getC() { return C; }
    }

    public static class GerchikovAverages {
        private final double INSTRUMENTAL, PROFESSIONAL, PATRIOTIC, MASTER, AVOIDING;
        
        public GerchikovAverages(double inst, double prof, double patr, double mast, double avoid) {
            this.INSTRUMENTAL = inst;
            this.PROFESSIONAL = prof;
            this.PATRIOTIC = patr;
            this.MASTER = mast;
            this.AVOIDING = avoid;
        }
        
        public double getInstrumental() { return INSTRUMENTAL; }
        public double getProfessional() { return PROFESSIONAL; }
        public double getPatriotic() { return PATRIOTIC; }
        public double getMaster() { return MASTER; }
        public double getAvoiding() { return AVOIDING; }
    }

    public interface DiscMetrics {
        double getD();
        double getI();
        double getS();
        double getC();
    }

    public interface GerchikovMetrics {
        double getInstrumental();
        double getProfessional();
        double getPatriotic();
        double getMaster();
        double getAvoiding();
    }
}
