package com.aifa.modules.ledger.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateCategoryRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "[a-z0-9_]+") String slug,
        String icon) {}
