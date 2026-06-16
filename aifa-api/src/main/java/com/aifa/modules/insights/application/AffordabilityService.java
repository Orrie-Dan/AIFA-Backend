package com.aifa.modules.insights.application;

import com.aifa.modules.insights.application.dto.AffordabilityRequest;
import com.aifa.modules.insights.application.dto.AffordabilityResponse;
import com.aifa.modules.insights.application.support.FinancialMetricsService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AffordabilityService {

    private final FinancialMetricsService metricsService;
    private final Clock clock;

    public AffordabilityService(FinancialMetricsService metricsService, Clock clock) {
        this.metricsService = metricsService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AffordabilityResponse check(UUID userId, AffordabilityRequest request) {
        LocalDate today = LocalDate.now(clock);
        int monthsAvailable = (int) ChronoUnit.MONTHS.between(today.withDayOfMonth(1), request.targetDate().withDayOfMonth(1));
        if (monthsAvailable < 0) {
            monthsAvailable = 0;
        }

        long liquid = metricsService.liquidSavingsRwf(userId);
        long monthlyIncome = metricsService.averageMonthlyIncomeRwf(userId, 3);
        long fixed = metricsService.estimateFixedExpensesRwf(userId, today);
        long variable = metricsService.averageMonthlyVariableExpensesRwf(userId, 3);
        long monthlyDisposable = Math.max(0, monthlyIncome - fixed - variable);

        long monthlyExpenses = fixed + variable;
        long emergencyTarget = monthlyExpenses * 3;
        long emergencyFundCurrent = liquid - emergencyTarget;
        if (emergencyFundCurrent < 0) {
            monthlyDisposable = Math.max(0, monthlyDisposable + emergencyFundCurrent / 12);
        }

        long projected = liquid + (monthlyDisposable * monthsAvailable);
        boolean affordable = projected >= request.itemPriceRwf();
        long buffer = projected - request.itemPriceRwf();

        int monthsNeeded = monthlyDisposable <= 0
                ? Integer.MAX_VALUE
                : (int) Math.ceil((double) Math.max(0, request.itemPriceRwf() - liquid) / monthlyDisposable);

        String emergencyImpact = "none";
        if (projected - request.itemPriceRwf() < emergencyTarget * 0.5) {
            emergencyImpact = "depleted";
        } else if (projected - request.itemPriceRwf() < emergencyTarget) {
            emergencyImpact = "partial";
        }

        double incomeVariance = metricsService.incomeVariancePercent3Mo(userId);
        String confidence;
        if (monthsAvailable >= 3 && incomeVariance < 15) {
            confidence = "high";
        } else if (monthsAvailable >= 2 || incomeVariance < 30) {
            confidence = "medium";
        } else {
            confidence = "low";
        }

        List<String> warnings = new ArrayList<>();
        if (incomeVariance >= 15) {
            warnings.add("Income has been irregular over the last 3 months");
        }
        if (monthlyDisposable == 0) {
            warnings.add("Little disposable income after fixed and variable expenses");
        }

        return new AffordabilityResponse(
                affordable,
                monthsAvailable,
                monthsNeeded == Integer.MAX_VALUE ? -1 : monthsNeeded,
                projected,
                buffer,
                emergencyImpact,
                confidence,
                warnings);
    }

    public Integer estimateMonthsToGoal(UUID userId, long remainingRwf) {
        if (remainingRwf <= 0) {
            return 0;
        }
        long disposable = metricsService.monthlyDisposableRwf(userId);
        if (disposable <= 0) {
            return null;
        }
        return (int) Math.ceil((double) remainingRwf / disposable);
    }
}
