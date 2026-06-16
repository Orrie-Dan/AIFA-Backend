package com.aifa.modules.ledger.application.dto;

import com.aifa.modules.ledger.domain.CategorySource;
import com.aifa.modules.ledger.domain.TransactionSource;
import com.aifa.modules.ledger.domain.TransactionType;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID walletId,
        long amountRwf,
        TransactionType type,
        UUID categoryId,
        CategorySource categorySource,
        String merchantName,
        String description,
        Instant transactionAt,
        TransactionSource source,
        Instant createdAt) {}
