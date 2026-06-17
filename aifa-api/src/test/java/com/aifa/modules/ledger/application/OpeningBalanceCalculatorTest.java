package com.aifa.modules.ledger.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.aifa.modules.importing.domain.SmsParsedRow;
import com.aifa.modules.ledger.domain.Wallet;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpeningBalanceCalculatorTest {

    @Test
    void derivesOpeningFromOutAnchor() {
        SmsParsedRow payment = row("OUT", 8_500L, 407_600L, "2026-07-13T20:12:00Z");

        long opening = OpeningBalanceCalculator.deriveOpeningFromAnchor(payment, List.of(payment), 0L);

        assertThat(opening).isEqualTo(416_100L);
    }

    @Test
    void derivesOpeningFromInAnchor() {
        SmsParsedRow receipt = row("IN", 50_000L, 1_250_000L, "2026-06-12T10:00:00Z");

        long opening = OpeningBalanceCalculator.deriveOpeningFromAnchor(receipt, List.of(receipt), 0L);

        assertThat(opening).isEqualTo(1_200_000L);
    }

    @Test
    void backfillShiftsOpeningWhenEarlierRowsAdded() {
        Wallet wallet = new Wallet();
        wallet.setOpeningBalanceRwf(100_000L);
        wallet.setLedgerFloorAt(Instant.parse("2026-07-01T00:00:00Z"));

        SmsParsedRow earlier = row("OUT", 5_000L, null, "2026-06-15T10:00:00Z");
        OpeningBalanceCalculator.applyBackfillFloorShift(
                wallet, List.of(earlier), Instant.parse("2026-06-15T10:00:00Z"));

        assertThat(wallet.getOpeningBalanceRwf()).isEqualTo(105_000L);
        assertThat(wallet.getLedgerFloorAt()).isEqualTo(Instant.parse("2026-06-15T10:00:00Z"));
    }

    private static SmsParsedRow row(String direction, long amount, Long balance, String at) {
        SmsParsedRow row = new SmsParsedRow();
        row.setDirection(direction);
        row.setAmountRwf(amount);
        row.setBalanceRwf(balance);
        row.setTransactionAt(Instant.parse(at));
        return row;
    }
}
