package com.aifa.modules.insights.application;

import com.aifa.modules.insights.application.dto.RecommendationResponse;
import com.aifa.modules.insights.application.dto.SpendingAnalysisResponse.CategoryTrend;
import com.aifa.modules.planning.application.BudgetService;
import com.aifa.modules.planning.application.GoalService;
import com.aifa.modules.planning.application.dto.BudgetResponse;
import com.aifa.modules.planning.application.dto.GoalResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {

    private final SpendingAnalysisService spendingAnalysisService;
    private final HealthScoreService healthScoreService;
    private final BudgetService budgetService;
    private final GoalService goalService;

    public RecommendationService(
            SpendingAnalysisService spendingAnalysisService,
            HealthScoreService healthScoreService,
            BudgetService budgetService,
            GoalService goalService) {
        this.spendingAnalysisService = spendingAnalysisService;
        this.healthScoreService = healthScoreService;
        this.budgetService = budgetService;
        this.goalService = goalService;
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(UUID userId) {
        List<RecommendationResponse> recommendations = new ArrayList<>();

        var health = healthScoreService.getCurrentScore(userId);
        if ("ready".equals(health.status()) && health.score() != null && health.score() < 60) {
            recommendations.add(new RecommendationResponse(
                    "health_score",
                    "Improve your financial health",
                    health.topImprovement(),
                    "medium"));
        }

        var spending = spendingAnalysisService.analyze(userId);
        for (CategoryTrend trend : spending.categories()) {
            if ("unusual".equals(trend.alertLevel()) && trend.zScore() > 0) {
                recommendations.add(new RecommendationResponse(
                        "spending_spike",
                        trend.categoryName() + " spending spike",
                        trend.alertMessage(),
                        "high"));
            }
            if (trend.momChangePercent() > 20) {
                recommendations.add(new RecommendationResponse(
                        "category_trend",
                        "Review " + trend.categoryName() + " budget",
                        String.format(
                                "%s spending is %.0f%% above your 3-month average",
                                trend.categoryName(), trend.momChangePercent()),
                        "medium"));
            }
        }

        for (BudgetResponse budget : budgetService.getCurrentBudgets(userId)) {
            if (budget.usagePercent() >= 90) {
                recommendations.add(new RecommendationResponse(
                        "budget_limit",
                        "Budget nearly exhausted",
                        String.format("You have used %.0f%% of a category budget", budget.usagePercent()),
                        budget.usagePercent() >= 100 ? "high" : "medium"));
            }
        }

        for (GoalResponse goal : goalService.listGoals(userId)) {
            if (goal.monthsToGoal() != null && goal.monthsToGoal() > 12) {
                recommendations.add(new RecommendationResponse(
                        "goal_pace",
                        "Goal needs more savings",
                        String.format(
                                "At your current pace, '%s' may take %d months", goal.name(), goal.monthsToGoal()),
                        "low"));
            }
        }

        return recommendations.stream().limit(10).toList();
    }
}
