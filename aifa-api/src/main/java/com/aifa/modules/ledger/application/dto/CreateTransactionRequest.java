package com.aifa.modules.ledger.application.dto;

import com.aifa.modules.ledger.domain.CategorySource;
import com.aifa.modules.ledger.domain.TransactionSource;
import com.aifa.modules.ledger.domain.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;

public record CreateTransactionRequest(
        @NotNull UUID walletId,
        @Positive long amountRwf,
        @NotNull TransactionType type,
        UUID categoryId,
        String merchantName,
        String description,
        @NotNull Instant transactionAt) {}
