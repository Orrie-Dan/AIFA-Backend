package com.aifa.modules.insights.application.dto;

import java.util.List;
import java.util.UUID;

public record SpendingAnalysisResponse(
        String status,
        List<CategoryTrend> categories,
        List<IncomeSuggestion> incomeSuggestions) {

    public record CategoryTrend(
            UUID categoryId,
            String categoryName,
            long currentMonthSpendRwf,
            long baselineSpendRwf,
            double momChangePercent,
            double zScore,
            String alertLevel,
            String alertMessage) {}

    public record IncomeSuggestion(
            String sourceLabel,
            int typicalDayOfMonth,
            long averageAmountRwf,
            int occurrenceCount,
            String message) {}
}
