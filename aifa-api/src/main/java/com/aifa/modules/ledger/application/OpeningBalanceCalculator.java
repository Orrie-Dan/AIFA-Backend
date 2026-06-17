package com.aifa.modules.ledger.application;

import com.aifa.modules.importing.domain.SmsParsedRow;
import com.aifa.modules.ledger.domain.Wallet;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Derives {@code opening_balance_rwf} from SMS anchor rows and handles ledger-floor shifts on backfill.
 *
 * <p>balance(wallet, t) = opening_balance_rwf + SUM(amount_rwf WHERE transaction_at &lt;= t)
 */
public final class OpeningBalanceCalculator {

    private OpeningBalanceCalculator() {}

    public static Optional<SmsParsedRow> findAnchorRow(List<SmsParsedRow> rows) {
        return rows.stream()
                .filter(row -> row.getBalanceRwf() != null)
                .min(Comparator.comparing(SmsParsedRow::getTransactionAt));
    }

    static long signedAmount(SmsParsedRow row) {
        return isIncome(row.getDirection()) ? row.getAmountRwf() : -row.getAmountRwf();
    }

    static long sumSignedUpTo(List<SmsParsedRow> rows, Instant inclusiveEnd) {
        return rows.stream()
                .filter(row -> !row.getTransactionAt().isAfter(inclusiveEnd))
                .mapToLong(OpeningBalanceCalculator::signedAmount)
                .sum();
    }

    public static long deriveOpeningFromAnchor(SmsParsedRow anchor, List<SmsParsedRow> newRows, long existingSumUpToAnchor) {
        long newRowsSum = sumSignedUpTo(newRows, anchor.getTransactionAt());
        return anchor.getBalanceRwf() - existingSumUpToAnchor - newRowsSum;
    }

    public static void applyBackfillFloorShift(Wallet wallet, List<SmsParsedRow> newRows, Instant earliestNewTxnAt) {
        if (!earliestNewTxnAt.isBefore(wallet.getLedgerFloorAt())) {
            return;
        }
        long earlierDelta = newRows.stream()
                .filter(row -> row.getTransactionAt().isBefore(wallet.getLedgerFloorAt()))
                .mapToLong(OpeningBalanceCalculator::signedAmount)
                .sum();
        wallet.setOpeningBalanceRwf(wallet.getOpeningBalanceRwf() - earlierDelta);
        wallet.setLedgerFloorAt(earliestNewTxnAt);
    }

    public static boolean isFirstImport(Wallet wallet, long existingTransactionCount) {
        return existingTransactionCount == 0
                && wallet.getOpeningBalanceRwf() == 0
                && wallet.getBalanceRwf() == 0;
    }

    private static boolean isIncome(String direction) {
        if (direction == null) {
            return false;
        }
        return "IN".equalsIgnoreCase(direction)
                || "credit".equalsIgnoreCase(direction)
                || "income".equalsIgnoreCase(direction);
    }
}
