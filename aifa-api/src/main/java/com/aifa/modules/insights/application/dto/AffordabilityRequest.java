package com.aifa.modules.insights.application.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record AffordabilityRequest(
        @Positive long itemPriceRwf, @Future LocalDate targetDate) {}
