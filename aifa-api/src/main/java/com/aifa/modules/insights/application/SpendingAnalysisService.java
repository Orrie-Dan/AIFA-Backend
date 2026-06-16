package com.aifa.modules.insights.application;

import com.aifa.modules.insights.application.dto.SpendingAnalysisResponse;
import com.aifa.modules.insights.application.dto.SpendingAnalysisResponse.CategoryTrend;
import com.aifa.modules.insights.application.dto.SpendingAnalysisResponse.IncomeSuggestion;
import com.aifa.modules.insights.application.support.FinancialMetricsService;
import com.aifa.modules.ledger.domain.Category;
import com.aifa.modules.ledger.domain.Transaction;
import com.aifa.modules.ledger.infrastructure.CategoryRepository;
import com.aifa.modules.ledger.infrastructure.TransactionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpendingAnalysisService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final FinancialMetricsService metricsService;
    private final Clock clock;

    public SpendingAnalysisService(
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            FinancialMetricsService metricsService,
            Clock clock) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.metricsService = metricsService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SpendingAnalysisResponse analyze(UUID userId) {
        if (!metricsService.hasMinimumHistory(userId, 7)) {
            return new SpendingAnalysisResponse("building_profile", List.of(), List.of());
        }

        LocalDate currentMonth = LocalDate.now(clock);
        List<Category> categories = categoryRepository.findByUserIdIsNullOrUserIdOrderByNameAsc(userId);
        List<CategoryTrend> trends = new ArrayList<>();

        for (Category category : categories) {
            long current = spendForMonth(userId, category.getId(), currentMonth);
            long m1 = spendForMonth(userId, category.getId(), currentMonth.minusMonths(1));
            long m2 = spendForMonth(userId, category.getId(), currentMonth.minusMonths(2));
            long m3 = spendForMonth(userId, category.getId(), currentMonth.minusMonths(3));

            if (current == 0 && m1 == 0 && m2 == 0 && m3 == 0) {
                continue;
            }

            double baseline = (m1 + m2 + m3) / 3.0;
            double std = stdDev(m1, m2, m3);
            double z = std > 0 ? (current - baseline) / std : 0;
            double mom = baseline == 0 ? 0 : ((current - baseline) / baseline) * 100.0;

            String alertLevel = "none";
            String alertMessage = null;
            if (Math.abs(z) > 2.5) {
                alertLevel = "unusual";
                alertMessage = z > 0
                        ? "Unusual spike in " + category.getName() + " spending"
                        : "Unusual drop in " + category.getName() + " spending";
            } else if (Math.abs(z) > 1.5) {
                alertLevel = "notable";
                alertMessage = z > 0
                        ? "Notable increase in " + category.getName()
                        : "Notable decrease in " + category.getName();
            }

            trends.add(new CategoryTrend(
                    category.getId(),
                    category.getName(),
                    current,
                    Math.round(baseline),
                    Math.round(mom * 10) / 10.0,
                    Math.round(z * 100) / 100.0,
                    alertLevel,
                    alertMessage));
        }

        trends.sort(Comparator.comparingLong(CategoryTrend::currentMonthSpendRwf).reversed());
        return new SpendingAnalysisResponse("ready", trends, detectIncomeSuggestions(userId));
    }

    public List<CategoryTrend> topAlerts(SpendingAnalysisResponse analysis, int limit) {
        return analysis.categories().stream()
                .filter(c -> !"none".equals(c.alertLevel()))
                .limit(limit)
                .toList();
    }

    private long spendForMonth(UUID userId, UUID categoryId, LocalDate month) {
        var range = metricsService.monthRange(month);
        return transactionRepository.sumExpensesForCategory(userId, categoryId, range.from(), range.to());
    }

    private double stdDev(long a, long b, long c) {
        double mean = (a + b + c) / 3.0;
        double variance = (Math.pow(a - mean, 2) + Math.pow(b - mean, 2) + Math.pow(c - mean, 2)) / 3.0;
        return Math.sqrt(variance);
    }

    private List<IncomeSuggestion> detectIncomeSuggestions(UUID userId) {
        Instant since = clock.instant().minus(90, ChronoUnit.DAYS);
        List<Transaction> incomes = transactionRepository.findIncomeSince(userId, since);
        if (incomes.size() < 2) {
            return List.of();
        }

        double avgAmount = incomes.stream().mapToLong(Transaction::getAmountRwf).average().orElse(0);
        long threshold = (long) (avgAmount * 5);

        Map<Integer, List<Transaction>> byDayCluster = new HashMap<>();
        for (Transaction tx : incomes) {
            if (tx.getAmountRwf() < threshold) {
                continue;
            }
            int day = LocalDate.ofInstant(tx.getTransactionAt(), ZoneOffset.UTC).getDayOfMonth();
            int clusterKey = ((day - 1) / 3) * 3 + 1;
            byDayCluster.computeIfAbsent(clusterKey, k -> new ArrayList<>()).add(tx);
        }

        List<IncomeSuggestion> suggestions = new ArrayList<>();
        for (Map.Entry<Integer, List<Transaction>> entry : byDayCluster.entrySet()) {
            if (entry.getValue().size() < 2) {
                continue;
            }
            long avg = (long) entry.getValue().stream()
                    .mapToLong(Transaction::getAmountRwf)
                    .average()
                    .orElse(0);
            String label = entry.getValue().get(0).getMerchantName();
            if (label == null || label.isBlank()) {
                label = entry.getValue().get(0).getDescription();
            }
            if (label == null || label.isBlank()) {
                label = "Recurring income";
            }
            suggestions.add(new IncomeSuggestion(
                    label,
                    entry.getKey(),
                    avg,
                    entry.getValue().size(),
                    "Is this your income from " + label + "?"));
        }
        return suggestions;
    }
}
