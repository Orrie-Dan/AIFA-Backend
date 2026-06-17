package com.aifa.modules.importing.application;

import com.aifa.modules.iam.application.AuthService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

final class SmsImportFingerprint {

    private SmsImportFingerprint() {}

    static String compute(
            UUID walletId,
            long amountRwf,
            String direction,
            String counterpartyName,
            Instant transactionAt,
            Long balanceRwf) {
        String counterparty = counterpartyName == null ? "" : counterpartyName.trim().toLowerCase();
        Instant rounded = transactionAt.truncatedTo(ChronoUnit.MINUTES);
        String balancePart = balanceRwf != null ? balanceRwf.toString() : "";
        String payload = walletId
                + "|"
                + amountRwf
                + "|"
                + direction
                + "|"
                + counterparty
                + "|"
                + rounded
                + "|"
                + balancePart;
        return AuthService.hashValue(payload);
    }
}
