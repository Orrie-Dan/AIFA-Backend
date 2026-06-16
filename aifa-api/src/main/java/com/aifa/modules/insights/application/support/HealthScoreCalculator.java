package com.aifa.modules.insights.application.support;

public final class HealthScoreCalculator {

    private HealthScoreCalculator() {}

    public record ComponentScores(
            int savingsRate,
            int emergencyFund,
            int budgetAdherence,
            int incomeStability,
            int debtServicing) {}

    public record WeightedResult(int totalScore, ComponentScores components, String topDriver, String topImprovement) {}

    public static int scoreSavingsRate(double savingsRate) {
        double pct = savingsRate * 100;
        if (pct >= 30) return 100;
        if (pct >= 20) return 80;
        if (pct >= 10) return 60;
        if (pct >= 5) return 40;
        if (pct >= 1) return 20;
        return 0;
    }

    public static int scoreEmergencyFund(double efMonths) {
        if (efMonths >= 3.0) return 100;
        if (efMonths >= 2.0) return 80;
        if (efMonths >= 1.0) return 55;
        if (efMonths >= 0.5) return 30;
        return 0;
    }

    public static int scoreBudgetAdherence(double adherenceRatio) {
        double pct = adherenceRatio * 100;
        if (pct >= 95) return 100;
        if (pct >= 85) return 80;
        if (pct >= 70) return 60;
        if (pct >= 55) return 40;
        return 20;
    }

    public static int scoreIncomeStability(double coefficientOfVariation) {
        if (coefficientOfVariation < 0.10) return 100;
        if (coefficientOfVariation < 0.20) return 80;
        if (coefficientOfVariation < 0.30) return 60;
        if (coefficientOfVariation < 0.50) return 40;
        return 20;
    }

    public static int scoreDebtServicing(double dsr) {
        double pct = dsr * 100;
        if (pct < 10) return 100;
        if (pct < 20) return 80;
        if (pct < 30) return 60;
        if (pct < 40) return 40;
        return 0;
    }

    public static String bandLabel(int score) {
        if (score >= 80) return "Strong";
        if (score >= 60) return "Building";
        if (score >= 40) return "Developing";
        return "Needs Attention";
    }

    public static WeightedResult compute(
            ComponentScores components, boolean includeBudget, boolean includeDebt) {
        double savingsW = 0.30;
        double emergencyW = 0.25;
        double budgetW = includeBudget ? 0.20 : 0.0;
        double incomeW = 0.15;
        double debtW = includeDebt ? 0.10 : 0.0;

        double excluded = (includeBudget ? 0 : 0.20) + (includeDebt ? 0 : 0.10);
        if (excluded > 0) {
            double boost = excluded / (savingsW + emergencyW + incomeW + budgetW + debtW);
            savingsW *= (1 + boost);
            emergencyW *= (1 + boost);
            incomeW *= (1 + boost);
            if (includeBudget) budgetW *= (1 + boost);
            if (includeDebt) debtW *= (1 + boost);
        }

        int total = (int) Math.round(
                components.savingsRate() * savingsW
                        + components.emergencyFund() * emergencyW
                        + components.budgetAdherence() * budgetW
                        + components.incomeStability() * incomeW
                        + components.debtServicing() * debtW);

        String topDriver = pickTopDriver(components);
        String topImprovement = pickTopImprovement(components);
        return new WeightedResult(Math.min(100, Math.max(0, total)), components, topDriver, topImprovement);
    }

    private static String pickTopDriver(ComponentScores c) {
        return maxComponent("Savings rate", c.savingsRate(), "Emergency fund", c.emergencyFund(), "Budget adherence", c.budgetAdherence());
    }

    private static String pickTopImprovement(ComponentScores c) {
        return minComponent("Increase savings rate", c.savingsRate(), "Build emergency fund", c.emergencyFund(), "Stick to budgets", c.budgetAdherence());
    }

    private static String maxComponent(String n1, int v1, String n2, int v2, String n3, int v3) {
        if (v1 >= v2 && v1 >= v3) return n1;
        if (v2 >= v3) return n2;
        return n3;
    }

    private static String minComponent(String n1, int v1, String n2, int v2, String n3, int v3) {
        if (v1 <= v2 && v1 <= v3) return n1;
        if (v2 <= v3) return n2;
        return n3;
    }
}
