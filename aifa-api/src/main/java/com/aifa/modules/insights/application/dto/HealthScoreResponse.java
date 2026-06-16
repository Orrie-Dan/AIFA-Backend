package com.aifa.modules.insights.application.dto;

import java.time.Instant;

public record HealthScoreResponse(
        String status,
        Integer score,
        String bandLabel,
        String topDriver,
        String topImprovement,
        ComponentBreakdown components,
        Instant computedAt) {

    public record ComponentBreakdown(
            Integer savingsRate,
            Integer emergencyFund,
            Integer budgetAdherence,
            Integer incomeStability,
            Integer debtServicing) {}
}
