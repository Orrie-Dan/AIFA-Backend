package com.aifa.modules.dashboard.application;

import com.aifa.modules.dashboard.application.dto.DashboardSummaryResponse;
import com.aifa.modules.insights.application.HealthScoreService;
import com.aifa.modules.insights.application.RecommendationService;
import com.aifa.modules.insights.application.SpendingAnalysisService;
import com.aifa.modules.ledger.application.WalletService;
import com.aifa.modules.ledger.application.dto.TransactionResponse;
import com.aifa.modules.ledger.infrastructure.TransactionRepository;
import com.aifa.modules.planning.application.BudgetService;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final WalletService walletService;
    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;
    private final HealthScoreService healthScoreService;
    private final SpendingAnalysisService spendingAnalysisService;
    private final RecommendationService recommendationService;

    public DashboardService(
            WalletService walletService,
            TransactionRepository transactionRepository,
            BudgetService budgetService,
            HealthScoreService healthScoreService,
            SpendingAnalysisService spendingAnalysisService,
            RecommendationService recommendationService) {
        this.walletService = walletService;
        this.transactionRepository = transactionRepository;
        this.budgetService = budgetService;
        this.healthScoreService = healthScoreService;
        this.spendingAnalysisService = spendingAnalysisService;
        this.recommendationService = recommendationService;
    }

    @Transactional
    public DashboardSummaryResponse getSummary(UUID userId) {
        var wallets = walletService.listWallets(userId);
        long totalBalance = wallets.stream().mapToLong(w -> w.balanceRwf()).sum();
        var recentTransactions = transactionRepository
                .findByUserIdOrderByTransactionAtDesc(userId, PageRequest.of(0, 10))
                .map(tx -> new TransactionResponse(
                        tx.getId(),
                        tx.getWalletId(),
                        tx.getAmountRwf(),
                        tx.getType(),
                        tx.getCategoryId(),
                        tx.getCategorySource(),
                        tx.getMerchantName(),
                        tx.getDescription(),
                        tx.getTransactionAt(),
                        tx.getSource(),
                        tx.getCreatedAt()));
        var budgets = budgetService.getCurrentBudgets(userId);
        var spending = spendingAnalysisService.analyze(userId);
        var alerts = spendingAnalysisService.topAlerts(spending, 3);

        return new DashboardSummaryResponse(
                totalBalance,
                wallets,
                recentTransactions.getContent(),
                budgets,
                healthScoreService.getCurrentScore(userId),
                alerts,
                recommendationService.getRecommendations(userId).stream().limit(3).toList());
    }
}
