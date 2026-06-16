package com.aifa.modules.importing.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ParsedSmsRow(
        int rowIndex,
        String rawText,
        boolean parsed,
        Long amountRwf,
        String counterpartyName,
        String phoneHash,
        Long balanceRwf,
        Instant transactionAt,
        String direction,
        String parseError) {

    public static ParsedSmsRow success(
            int rowIndex,
            String rawText,
            long amountRwf,
            String counterpartyName,
            String phone,
            Long balanceRwf,
            Instant transactionAt,
            String direction) {
        String phoneHash = phone != null ? hashPhoneStatic(phone) : null;
        return new ParsedSmsRow(
                rowIndex,
                rawText,
                true,
                amountRwf,
                counterpartyName,
                phoneHash,
                balanceRwf,
                transactionAt,
                direction,
                null);
    }

    public static ParsedSmsRow failure(int rowIndex, String rawText, String error) {
        return new ParsedSmsRow(rowIndex, rawText, false, null, null, null, null, null, null, error);
    }

    private static String hashPhoneStatic(String phone) {
        return com.aifa.modules.iam.application.AuthService.hashValue(phone);
    }
}
