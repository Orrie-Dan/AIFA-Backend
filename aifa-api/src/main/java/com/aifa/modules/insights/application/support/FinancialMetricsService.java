package com.aifa.modules.insights.application.support;

import com.aifa.modules.ledger.infrastructure.TransactionRepository;
import com.aifa.modules.ledger.infrastructure.WalletRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class FinancialMetricsService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final Clock clock;

    public FinancialMetricsService(
            TransactionRepository transactionRepository, WalletRepository walletRepository, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.clock = clock;
    }

    public boolean hasMinimumHistory(UUID userId, int days) {
        return transactionRepository
                .findEarliestTransactionAt(userId)
                .map(earliest -> ChronoUnit.DAYS.between(earliest, clock.instant()) >= days)
                .orElse(false);
    }

    public long liquidSavingsRwf(UUID userId) {
        return walletRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .mapToLong(w -> w.getBalanceRwf())
                .sum();
    }

    public long monthlyIncomeRwf(UUID userId, LocalDate month) {
        MonthRange range = monthRange(month);
        return transactionRepository.sumIncome(userId, range.from(), range.to());
    }

    public long monthlyExpensesRwf(UUID userId, LocalDate month) {
        MonthRange range = monthRange(month);
        return transactionRepository.sumExpenses(userId, range.from(), range.to());
    }

    public long averageMonthlyIncomeRwf(UUID userId, int months) {
        return averageOverMonths(userId, months, true);
    }

    public long averageMonthlyExpensesRwf(UUID userId, int months) {
        return averageOverMonths(userId, months, false);
    }

    public long averageMonthlyVariableExpensesRwf(UUID userId, int months) {
        LocalDate current = LocalDate.now(clock);
        long total = 0;
        for (int i = 1; i <= months; i++) {
            LocalDate month = current.minusMonths(i);
            total += monthlyExpensesRwf(userId, month) - estimateFixedExpensesRwf(userId, month);
        }
        return months == 0 ? 0 : total / months;
    }

    public long estimateFixedExpensesRwf(UUID userId, LocalDate month) {
        // Approximate fixed costs as rent + utilities category spend for the month.
        // Full category-slug join deferred; use 40% of expenses as fixed heuristic when no budgets.
        long expenses = monthlyExpensesRwf(userId, month);
        return Math.round(expenses * 0.4);
    }

    public double incomeCoefficientOfVariation(UUID userId, int months) {
        LocalDate current = LocalDate.now(clock);
        List<Long> incomes = new ArrayList<>();
        for (int i = 0; i < months; i++) {
            incomes.add(monthlyIncomeRwf(userId, current.minusMonths(i)));
        }
        double mean = incomes.stream().mapToLong(Long::longValue).average().orElse(0);
        if (mean == 0) {
            return 0;
        }
        double variance = incomes.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);
        return Math.sqrt(variance) / mean;
    }

    public double incomeVariancePercent3Mo(UUID userId) {
        return incomeCoefficientOfVariation(userId, 3) * 100.0;
    }

    public long monthlyDisposableRwf(UUID userId) {
        long income = averageMonthlyIncomeRwf(userId, 3);
        LocalDate current = LocalDate.now(clock);
        long fixed = estimateFixedExpensesRwf(userId, current);
        long variable = averageMonthlyVariableExpensesRwf(userId, 3);
        return Math.max(0, income - fixed - variable);
    }

    public MonthRange monthRange(LocalDate month) {
        LocalDate start = month.withDayOfMonth(1);
        LocalDate end = start.plusMonths(1);
        return new MonthRange(
                start.atStartOfDay(ZoneOffset.UTC).toInstant(),
                end.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    public List<MonthRange> lastMonths(int count) {
        LocalDate current = LocalDate.now(clock);
        List<MonthRange> ranges = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ranges.add(monthRange(current.minusMonths(i)));
        }
        return ranges;
    }

    public Optional<Instant> earliestTransactionAt(UUID userId) {
        return transactionRepository.findEarliestTransactionAt(userId);
    }

    private long averageOverMonths(UUID userId, int months, boolean income) {
        LocalDate current = LocalDate.now(clock);
        long total = 0;
        for (int i = 0; i < months; i++) {
            LocalDate month = current.minusMonths(i);
            total += income ? monthlyIncomeRwf(userId, month) : monthlyExpensesRwf(userId, month);
        }
        return months == 0 ? 0 : total / months;
    }

    public record MonthRange(Instant from, Instant to) {}
}
