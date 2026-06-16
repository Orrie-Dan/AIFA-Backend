package com.aifa.modules.ledger.application.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateTransactionCategoryRequest(@NotNull UUID categoryId) {}
