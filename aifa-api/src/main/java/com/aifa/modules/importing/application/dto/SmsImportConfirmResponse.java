package com.aifa.modules.importing.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SmsImportConfirmResponse(
        UUID batchId,
        int importedCount,
        List<UUID> transactionIds,
        long openingBalanceRwf,
        long computedBalanceRwf,
        Instant ledgerFloorAt,
        List<SmsBalanceDiscrepancy> discrepancies,
        List<Integer> alreadyImportedRowIndexes) {}
