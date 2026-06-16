package com.aifa.modules.planning.application;

import com.aifa.modules.planning.application.dto.BudgetResponse;
import com.aifa.modules.planning.application.dto.CreateBudgetRequest;
import com.aifa.modules.planning.application.dto.UpdateBudgetRequest;
import com.aifa.modules.planning.domain.Budget;
import com.aifa.modules.planning.domain.BudgetPeriod;
import com.aifa.modules.planning.infrastructure.BudgetRepository;
import com.aifa.modules.ledger.infrastructure.CategoryRepository;
import com.aifa.modules.ledger.infrastructure.TransactionRepository;
import com.aifa.shared.audit.AuditLogger;
import com.aifa.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public BudgetService(
            BudgetRepository budgetRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            AuditLogger auditLogger,
            Clock clock) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getCurrentBudgets(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        return budgetRepository.findActiveForDate(userId, today).stream()
                .map(budget -> toResponse(userId, budget, today))
                .toList();
    }

    @Transactional
    public BudgetResponse createBudget(UUID userId, CreateBudgetRequest request) {
        categoryRepository
                .findById(request.categoryId())
                .filter(category -> category.getUserId() == null || category.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setCategoryId(request.categoryId());
        budget.setAmountRwf(request.amountRwf());
        budget.setPeriod(request.period());
        budget.setActiveFrom(request.activeFrom());
        budget.setActiveTo(request.activeTo());
        budget.setCreatedAt(clock.instant());
        budget.setUpdatedAt(clock.instant());
        budgetRepository.save(budget);

        auditLogger.logAction(userId, "CREATE", "budget", budget.getId().toString());
        return toResponse(userId, budget, LocalDate.now(clock));
    }

    @Transactional
    public BudgetResponse updateBudget(UUID userId, UUID budgetId, UpdateBudgetRequest request) {
        Budget budget = budgetRepository
                .findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (request.amountRwf() != null) {
            budget.setAmountRwf(request.amountRwf());
        }
        if (request.activeTo() != null) {
            budget.setActiveTo(request.activeTo());
        }
        budget.setUpdatedAt(clock.instant());
        budgetRepository.save(budget);
        auditLogger.logAction(userId, "UPDATE", "budget", budgetId.toString());
        return toResponse(userId, budget, LocalDate.now(clock));
    }

    private BudgetResponse toResponse(UUID userId, Budget budget, LocalDate referenceDate) {
        PeriodRange range = resolvePeriodRange(budget.getPeriod(), referenceDate);
        long spent = transactionRepository.sumExpensesForCategory(
                userId, budget.getCategoryId(), range.from(), range.to());
        double usage = budget.getAmountRwf() == 0 ? 0 : (spent * 100.0) / budget.getAmountRwf();
        return new BudgetResponse(
                budget.getId(),
                budget.getCategoryId(),
                budget.getAmountRwf(),
                budget.getPeriod(),
                budget.getActiveFrom(),
                budget.getActiveTo(),
                spent,
                Math.min(usage, 999.99));
    }

    private PeriodRange resolvePeriodRange(BudgetPeriod period, LocalDate referenceDate) {
        if (period == BudgetPeriod.weekly) {
            LocalDate start = referenceDate.minusDays(referenceDate.getDayOfWeek().getValue() - 1L);
            Instant from = start.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant to = start.plusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant();
            return new PeriodRange(from, to);
        }
        LocalDate start = referenceDate.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate end = start.plusMonths(1);
        return new PeriodRange(
                start.atStartOfDay(ZoneOffset.UTC).toInstant(),
                end.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private record PeriodRange(Instant from, Instant to) {}
}
