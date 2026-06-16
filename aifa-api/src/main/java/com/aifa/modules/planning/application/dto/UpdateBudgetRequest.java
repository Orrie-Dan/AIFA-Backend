package com.aifa.modules.planning.application.dto;

import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record UpdateBudgetRequest(@Positive Long amountRwf, LocalDate activeTo) {}
