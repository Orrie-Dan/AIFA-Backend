package com.aifa.modules.planning.application.dto;

import com.aifa.modules.planning.domain.GoalStatus;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        String name,
        long targetRwf,
        long currentRwf,
        double progressPercent,
        LocalDate targetDate,
        GoalStatus status,
        Integer monthsToGoal) {}
