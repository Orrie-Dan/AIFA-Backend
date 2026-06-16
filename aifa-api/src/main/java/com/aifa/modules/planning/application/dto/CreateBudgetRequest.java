package com.aifa.modules.planning.application.dto;

import com.aifa.modules.planning.domain.BudgetPeriod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.UUID;

public record CreateBudgetRequest(
        @NotNull UUID categoryId,
        @Positive long amountRwf,
        @NotNull BudgetPeriod period,
        @NotNull LocalDate activeFrom,
        LocalDate activeTo) {}
