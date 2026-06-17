package com.aifa.modules.importing.application.dto;

public record SmsBalanceDiscrepancy(
        int rowIndex, long reportedBalanceRwf, long expectedBalanceRwf, long deltaRwf) {}
