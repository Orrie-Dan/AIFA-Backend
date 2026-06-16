package com.aifa.modules.insights.application.dto;

public record DashboardInsightsSnapshot(
        HealthScoreResponse healthScore, SpendingAnalysisResponse spendingHighlights) {}
