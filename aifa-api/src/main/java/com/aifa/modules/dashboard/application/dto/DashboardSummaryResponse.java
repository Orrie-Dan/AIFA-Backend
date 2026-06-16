package com.aifa.modules.dashboard.application.dto;

import com.aifa.modules.insights.application.dto.HealthScoreResponse;
import com.aifa.modules.insights.application.dto.RecommendationResponse;
import com.aifa.modules.insights.application.dto.SpendingAnalysisResponse.CategoryTrend;
import com.aifa.modules.ledger.application.dto.TransactionResponse;
import com.aifa.modules.ledger.application.dto.WalletResponse;
import com.aifa.modules.planning.application.dto.BudgetResponse;
import java.util.List;

public record DashboardSummaryResponse(
        long totalBalanceRwf,
        List<WalletResponse> wallets,
        List<TransactionResponse> recentTransactions,
        List<BudgetResponse> budgetGauges,
        HealthScoreResponse healthScore,
        List<CategoryTrend> spendingAlerts,
        List<RecommendationResponse> recommendations) {}
