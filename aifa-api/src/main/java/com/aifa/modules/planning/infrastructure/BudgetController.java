package com.aifa.modules.planning.infrastructure;

import com.aifa.modules.planning.application.BudgetService;
import com.aifa.modules.planning.application.dto.BudgetResponse;
import com.aifa.modules.planning.application.dto.CreateBudgetRequest;
import com.aifa.modules.planning.application.dto.UpdateBudgetRequest;
import com.aifa.shared.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final CurrentUserProvider currentUserProvider;

    public BudgetController(BudgetService budgetService, CurrentUserProvider currentUserProvider) {
        this.budgetService = budgetService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/current")
    public List<BudgetResponse> getCurrentBudgets() {
        return budgetService.getCurrentBudgets(currentUserProvider.getCurrentUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse createBudget(@Valid @RequestBody CreateBudgetRequest request) {
        return budgetService.createBudget(currentUserProvider.getCurrentUserId(), request);
    }

    @PatchMapping("/{id}")
    public BudgetResponse updateBudget(@PathVariable UUID id, @Valid @RequestBody UpdateBudgetRequest request) {
        return budgetService.updateBudget(currentUserProvider.getCurrentUserId(), id, request);
    }
}
