package com.aifa.modules.importing.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.aifa.modules.importing.application.dto.ParsedSmsRow;
import com.aifa.shared.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SmsParseServiceTest {

    private final SmsParseService smsParseService = new SmsParseService();

    @Test
    void parsesMtnMomoReceivedSms() {
        String sms =
                "You have received RWF 50,000 from JOHN DOE (0781234567). Your new balance is RWF 1,250,000.";

        List<ParsedSmsRow> rows = smsParseService.parseBatch(sms, Instant.parse("2025-06-12T10:00:00Z"));

        assertThat(rows).hasSize(1);
        ParsedSmsRow row = rows.getFirst();
        assertThat(row.parsed()).isTrue();
        assertThat(row.amountRwf()).isEqualTo(50_000L);
        assertThat(row.counterpartyName()).isEqualTo("JOHN DOE");
        assertThat(row.balanceRwf()).isEqualTo(1_250_000L);
        assertThat(row.direction()).isEqualTo("IN");
        assertThat(row.phoneHash()).isNotBlank();
    }

    @Test
    void parsesMtnMomoSentSms() {
        String sms = "You have transferred RWF 25,000 to MARY SMITH. Your new balance is RWF 975,000.";

        List<ParsedSmsRow> rows = smsParseService.parseBatch(sms, Instant.parse("2025-06-12T10:00:00Z"));

        assertThat(rows).hasSize(1);
        ParsedSmsRow row = rows.getFirst();
        assertThat(row.parsed()).isTrue();
        assertThat(row.amountRwf()).isEqualTo(25_000L);
        assertThat(row.counterpartyName()).isEqualTo("MARY SMITH");
        assertThat(row.direction()).isEqualTo("OUT");
    }

    @Test
    void returnsParseErrorForUnknownFormat() {
        String sms = "Your airtime bundle has been activated.";

        List<ParsedSmsRow> rows = smsParseService.parseBatch(sms, Instant.now());

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().parsed()).isFalse();
        assertThat(rows.getFirst().parseError()).contains("mtn_momo_v2");
    }

    @Test
    void returnsAirtelErrorCode() {
        String sms = "Airtel Money: Payment of RWF 5000 to Shop was successful.";

        List<ParsedSmsRow> rows = smsParseService.parseBatch(sms, Instant.now());

        assertThat(rows.getFirst().parseError()).isEqualTo(ErrorCode.AIRTEL_NOT_SUPPORTED_FOR_MTN_WALLET);
    }
}
