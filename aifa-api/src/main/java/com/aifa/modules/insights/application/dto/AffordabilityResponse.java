package com.aifa.modules.insights.application.dto;

import java.util.List;

public record AffordabilityResponse(
        boolean affordable,
        int monthsAvailable,
        int monthsNeededMinimum,
        long projectedSavingsAtTargetDateRwf,
        long bufferAfterPurchaseRwf,
        String emergencyFundImpact,
        String confidence,
        List<String> warnings) {}
