package com.aifa.modules.planning.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record CreateGoalRequest(
        @NotBlank String name, @Positive long targetRwf, LocalDate targetDate) {}
