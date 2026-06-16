package com.aifa.modules.planning.application.dto;

import com.aifa.modules.planning.domain.GoalStatus;
import java.time.LocalDate;

public record UpdateGoalRequest(String name, Long targetRwf, LocalDate targetDate, GoalStatus status) {}
