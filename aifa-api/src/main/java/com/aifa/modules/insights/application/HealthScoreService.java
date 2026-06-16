package com.aifa.modules.insights.application;

import com.aifa.modules.insights.application.dto.HealthScoreResponse;
import com.aifa.modules.insights.application.support.FinancialMetricsService;
import com.aifa.modules.insights.application.support.HealthScoreCalculator;
import com.aifa.modules.insights.application.support.HealthScoreCalculator.ComponentScores;
import com.aifa.modules.insights.application.support.HealthScoreCalculator.WeightedResult;
import com.aifa.modules.insights.domain.HealthScore;
import com.aifa.modules.insights.infrastructure.HealthScoreRepository;
import com.aifa.modules.planning.application.BudgetService;
import com.aifa.modules.planning.application.dto.BudgetResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HealthScoreService {

    private final HealthScoreRepository healthScoreRepository;
    private final FinancialMetricsService metricsService;
    private final BudgetService budgetService;
    private final Clock clock;

    public HealthScoreService(
            HealthScoreRepository healthScoreRepository,
            FinancialMetricsService metricsService,
            BudgetService budgetService,
            Clock clock) {
        this.healthScoreRepository = healthScoreRepository;
        this.metricsService = metricsService;
        this.budgetService = budgetService;
        this.clock = clock;
    }

    @Transactional
    public HealthScoreResponse getCurrentScore(UUID userId) {
        if (!metricsService.hasMinimumHistory(userId, 30)) {
            return buildingProfile();
        }
        return healthScoreRepository
                .findFirstByUserIdOrderByComputedAtDesc(userId)
                .map(this::toResponse)
                .orElseGet(() -> toResponse(computeAndPersistInternal(userId)));
    }

    @Transactional
    public HealthScore computeAndPersist(UUID userId) {
        return computeAndPersistInternal(userId);
    }

    private HealthScore computeAndPersistInternal(UUID userId) {
        WeightedResult result = calculate(userId);
        HealthScore entity = new HealthScore();
        entity.setUserId(userId);
        entity.setScore((short) result.totalScore());
        entity.setSavingsRate((short) result.components().savingsRate());
        entity.setEmergencyFund((short) result.components().emergencyFund());
        entity.setBudgetAdherence((short) result.components().budgetAdherence());
        entity.setIncomeStability((short) result.components().incomeStability());
        entity.setDebtServicing((short) result.components().debtServicing());
        entity.setBandLabel(HealthScoreCalculator.bandLabel(result.totalScore()));
        entity.setTopDriver(result.topDriver());
        entity.setTopImprovement(result.topImprovement());
        entity.setComputedAt(clock.instant());
        return healthScoreRepository.save(entity);
    }

    public WeightedResult calculate(UUID userId) {
        LocalDate current = LocalDate.now(clock);
        long income = metricsService.monthlyIncomeRwf(userId, current);
        long expenses = metricsService.monthlyExpensesRwf(userId, current);
        long savings = income - expenses;
        double savingsRate = income == 0 ? 0 : (double) savings / income;

        long liquid = metricsService.liquidSavingsRwf(userId);
        double avgExpenses = metricsService.averageMonthlyExpensesRwf(userId, 3);
        double efMonths = avgExpenses == 0 ? 0 : (double) liquid / avgExpenses;

        List<BudgetResponse> budgets = budgetService.getCurrentBudgets(userId);
        boolean includeBudget = !budgets.isEmpty();
        double budgetAdherence = 0;
        if (includeBudget) {
            double weighted = 0;
            double weightSum = 0;
            for (BudgetResponse budget : budgets) {
                double adherence = budget.amountRwf() == 0
                        ? 1.0
                        : Math.min(1.0, (double) budget.amountRwf() / Math.max(1, budget.spentRwf()));
                weighted += adherence * budget.amountRwf();
                weightSum += budget.amountRwf();
            }
            budgetAdherence = weightSum == 0 ? 1.0 : weighted / weightSum;
        }

        double cv = metricsService.incomeCoefficientOfVariation(userId, 6);
        boolean includeDebt = false;
        double dsr = 0;

        ComponentScores components = new ComponentScores(
                HealthScoreCalculator.scoreSavingsRate(savingsRate),
                HealthScoreCalculator.scoreEmergencyFund(efMonths),
                includeBudget ? HealthScoreCalculator.scoreBudgetAdherence(budgetAdherence) : 0,
                HealthScoreCalculator.scoreIncomeStability(cv),
                includeDebt ? HealthScoreCalculator.scoreDebtServicing(dsr) : 100);

        return HealthScoreCalculator.compute(components, includeBudget, includeDebt);
    }

    private HealthScoreResponse buildingProfile() {
        return new HealthScoreResponse(
                "building_profile", null, null, null, null, null, null);
    }

    private HealthScoreResponse toResponse(HealthScore score) {
        return new HealthScoreResponse(
                "ready",
                (int) score.getScore(),
                score.getBandLabel(),
                score.getTopDriver(),
                score.getTopImprovement(),
                new HealthScoreResponse.ComponentBreakdown(
                        (int) score.getSavingsRate(),
                        (int) score.getEmergencyFund(),
                        (int) score.getBudgetAdherence(),
                        (int) score.getIncomeStability(),
                        (int) score.getDebtServicing()),
                score.getComputedAt());
    }
}
