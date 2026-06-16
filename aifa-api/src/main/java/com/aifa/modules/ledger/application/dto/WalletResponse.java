package com.aifa.modules.ledger.application.dto;

import com.aifa.modules.ledger.domain.TransactionType;
import com.aifa.modules.ledger.domain.WalletType;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
        UUID id, String name, WalletType type, long balanceRwf, boolean primary, Instant createdAt) {}
