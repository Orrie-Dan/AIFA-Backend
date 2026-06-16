package com.aifa.modules.planning.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ContributeGoalRequest(@Positive long amountRwf, String note) {}
