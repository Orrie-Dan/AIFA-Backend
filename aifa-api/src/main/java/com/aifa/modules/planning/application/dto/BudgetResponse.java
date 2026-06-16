package com.aifa.modules.planning.application.dto;

import com.aifa.modules.planning.domain.BudgetPeriod;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        UUID categoryId,
        long amountRwf,
        BudgetPeriod period,
        LocalDate activeFrom,
        LocalDate activeTo,
        long spentRwf,
        double usagePercent) {}
