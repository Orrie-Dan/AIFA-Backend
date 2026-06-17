package com.aifa.modules.importing.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.aifa.modules.importing.application.dto.ParsedSmsRow;
import com.aifa.shared.exception.ErrorCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class MtnMomoSmsParserTest {

    private MtnMomoSmsParser parser;
    private SmsParseService smsParseService;
    private Instant defaultTimestamp;

    @BeforeEach
    void setUp() {
        parser = new MtnMomoSmsParser();
        smsParseService = new SmsParseService();
        defaultTimestamp = Instant.parse("2025-06-12T10:00:00Z");
    }

    @Test
    void parsesReceivedWithPhoneAndBalance() {
        var result = parser.parse(
                "You have received RWF 50,000 from JOHN DOE (0781234567). Your new balance is RWF 1,250,000.",
                defaultTimestamp);

        assertThat(result.parsed()).isTrue();
        assertThat(result.amountRwf()).isEqualTo(50_000L);
        assertThat(result.counterpartyName()).isEqualTo("JOHN DOE");
        assertThat(result.direction()).isEqualTo("IN");
        assertThat(result.balanceRwf()).isEqualTo(1_250_000L);
    }

    @Test
    void parsesYouHavePaid() {
        var result = parser.parse(
                "MTN MoMo: You have paid RWF 8,500 to Heaven Restaurant Kimironko. Remaining balance: RWF 407,600",
                defaultTimestamp);

        assertThat(result.parsed()).isTrue();
        assertThat(result.amountRwf()).isEqualTo(8_500L);
        assertThat(result.counterpartyName()).isEqualTo("Heaven Restaurant Kimironko");
        assertThat(result.direction()).isEqualTo("OUT");
        assertThat(result.balanceRwf()).isEqualTo(407_600L);
    }

    @Test
    void parsesPaymentSuccessfulCompressed() {
        var result = parser.parse(
                "Payment successful. RWF 8500 paid to Heaven Restaurant Kimironko on 13/07/2026 20:12. Remaining balance: RWF 366,400",
                defaultTimestamp);

        assertThat(result.parsed()).isTrue();
        assertThat(result.amountRwf()).isEqualTo(8_500L);
        assertThat(result.counterpartyName()).isEqualTo("Heaven Restaurant Kimironko");
        assertThat(result.transactionAt()).isEqualTo(Instant.parse("2026-07-13T18:12:00Z"));
    }

    @Test
    void parsesMerchantPaymentAndAirtime() {
        assertThat(parser.parse(
                        "Merchant payment RWF 25000 to Simba Supermarket on 13/07/2026 at 14:30. Available balance RWF 365,600",
                        defaultTimestamp)
                .parsed())
                .isTrue();
        assertThat(parser.parse("You bought airtime worth RWF 1,000. Your new balance is RWF 364,600", defaultTimestamp)
                        .direction())
                .isEqualTo("OUT");
        assertThat(parser.parse("You bought airtime of RWF 2000 to 0788123456. Balance RWF 362,600", defaultTimestamp)
                        .parsed())
                .isTrue();
    }

    @Test
    void parsesBankTransfers() {
        var out = parser.parse(
                "Transfer to Bank BK Account 1234567. Amount RWF 100,000 on 10/07/2026 at 09:15. Your account balance is RWF 262,600",
                defaultTimestamp);
        assertThat(out.parsed()).isTrue();
        assertThat(out.direction()).isEqualTo("OUT");
        assertThat(out.counterpartyName()).contains("Bank");

        var inflow = parser.parse(
                "Transfer from Bank Equity Account. Amount RWF 300000 on 01/07/2026 at 08:00. Balance RWF 1,120,100",
                defaultTimestamp);
        assertThat(inflow.parsed()).isTrue();
        assertThat(inflow.direction()).isEqualTo("IN");
    }

    @Test
    void parsesMoneyReceivedCompressed() {
        var result = parser.parse("Money received RWF 150000 from ACME Corp. Balance RWF 516,400", defaultTimestamp);

        assertThat(result.parsed()).isTrue();
        assertThat(result.direction()).isEqualTo("IN");
        assertThat(result.counterpartyName()).isEqualTo("ACME Corp");
    }

    @Test
    void parsesReversalsSalaryAndFreelance() {
        assertThat(parser.parse("Reversal of RWF 2000 received. Your new balance is RWF 565,100", defaultTimestamp)
                        .direction())
                .isEqualTo("IN");
        assertThat(parser.parse("Refund RWF 5000 from Online Store. Balance RWF 570,100", defaultTimestamp)
                        .parsed())
                .isTrue();
        assertThat(parser.parse(
                        "Salary payment RWF 200000 received from Employer Ltd. Your new balance is RWF 770,100",
                        defaultTimestamp)
                        .direction())
                .isEqualTo("IN");
        assertThat(parser.parse("Freelance payment RWF 50000 received from Client ABC. Balance RWF 820,100", defaultTimestamp)
                        .parsed())
                .isTrue();
    }

    @Test
    void normalizesFrwPrefixAndReversedAmount() {
        assertThat(parser.parse("Payment of FRW 12300 to Pharmacy was successful. Your new balance is RWF 571,600", defaultTimestamp)
                        .amountRwf())
                .isEqualTo(12_300L);
        assertThat(parser.parse("8500 RWF paid to Restaurant. Remaining balance: RWF 563,100", defaultTimestamp)
                        .parsed())
                .isTrue();
    }

    @Test
    void rejectsAirtelWithExplicitCode() {
        var result = parser.parse("Airtel Money: You have received RWF 10000 from Friend", defaultTimestamp);

        assertThat(result.parsed()).isFalse();
        assertThat(result.parseError()).isEqualTo(ErrorCode.AIRTEL_NOT_SUPPORTED_FOR_MTN_WALLET);
    }

    @Test
    void parsesRealMomoTransferWithFee() {
        var result = parser.parse(
                "*165*S*1500 RWF transferred to Jack TURIKUMANA (250783955909) at 2026-06-01 09:16:12 .Fee: 100RWF.Balance: 4383RWF.",
                defaultTimestamp);

        assertThat(result.parsed()).isTrue();
        assertThat(result.amountRwf()).isEqualTo(1_600L);
        assertThat(result.counterpartyName()).isEqualTo("Jack TURIKUMANA");
        assertThat(result.direction()).isEqualTo("OUT");
        assertThat(result.balanceRwf()).isEqualTo(4_383L);
        assertThat(result.transactionAt()).isEqualTo(Instant.parse("2026-06-01T07:16:12Z"));
    }

    @Test
    void parsesRealMomoReceivedWithMaskedPhone() {
        var result = parser.parse(
                "You have received 4700 RWF from FIDOTECH RWANDA Ltd (*********965) at 2026-06-02 09:02:05. Balance:4963 RWF.",
                defaultTimestamp);

        assertThat(result.parsed()).isTrue();
        assertThat(result.amountRwf()).isEqualTo(4_700L);
        assertThat(result.direction()).isEqualTo("IN");
        assertThat(result.balanceRwf()).isEqualTo(4_963L);
    }

    @Test
    void parsesRealMomoMerchantDebit() {
        var result = parser.parse(
                "*164*S*Y'ello, A transaction of 1500 RWF by FIDOTECH RWANDA Ltd was completed at 2026-06-01 14:24:57. Balance:1863 RWF.",
                defaultTimestamp);

        assertThat(result.parsed()).isTrue();
        assertThat(result.amountRwf()).isEqualTo(1_500L);
        assertThat(result.counterpartyName()).contains("FIDOTECH");
        assertThat(result.direction()).isEqualTo("OUT");
    }

    @Test
    void parsesRealMomoTxIdMerchantPayment() {
        var result = parser.parse(
                "TxId:28597730434*S*Your payment of 1,500 RWF to Jean Bosco 20961 was completed at 2026-06-17 09:15:19.  Balance: 3,223 RWF.",
                defaultTimestamp);

        assertThat(result.parsed()).isTrue();
        assertThat(result.amountRwf()).isEqualTo(1_500L);
        assertThat(result.counterpartyName()).contains("Jean Bosco");
    }

    @Test
    void realUserCorpusRegression() throws IOException {
        String corpus = new ClassPathResource("sms/real_momo_corpus.txt")
                .getContentAsString(StandardCharsets.UTF_8);
        List<ParsedSmsRow> rows = smsParseService.parseBatch(corpus, defaultTimestamp);

        List<ParsedSmsRow> transactional = rows.stream()
                .filter(row -> !row.rawText().toLowerCase().contains("umaze kugura"))
                .toList();
        long parsed = transactional.stream().filter(ParsedSmsRow::parsed).count();

        assertThat(transactional).hasSize(78);
        assertThat(parsed).as("Real MoMo corpus parse rate").isGreaterThanOrEqualTo((long) Math.ceil(transactional.size() * 0.95));
    }

    @Test
    void fullCorpusRegression() throws IOException {
        String corpus = new ClassPathResource("sms/mtn_momo_corpus.txt")
                .getContentAsString(StandardCharsets.UTF_8);
        List<ParsedSmsRow> rows = smsParseService.parseBatch(corpus, defaultTimestamp);

        long mtnParsed = rows.stream()
                .filter(row -> !row.rawText().toLowerCase().contains("airtel"))
                .filter(row -> !row.rawText().contains("airtime bundle has been activated"))
                .filter(row -> !row.rawText().startsWith("Welcome to MTN"))
                .filter(ParsedSmsRow::parsed)
                .count();
        long mtnTotal = rows.stream()
                .filter(row -> !row.rawText().toLowerCase().contains("airtel"))
                .filter(row -> !row.rawText().contains("airtime bundle has been activated"))
                .filter(row -> !row.rawText().startsWith("Welcome to MTN"))
                .count();

        long airtelRejected = rows.stream()
                .filter(row -> row.rawText().toLowerCase().contains("airtel"))
                .filter(row -> ErrorCode.AIRTEL_NOT_SUPPORTED_FOR_MTN_WALLET.equals(row.parseError()))
                .count();

        assertThat(mtnTotal).isEqualTo(40);
        assertThat(mtnParsed).as("MTN parse rate should be >= 90%%").isGreaterThanOrEqualTo((long) Math.ceil(mtnTotal * 0.9));
        assertThat(airtelRejected).isEqualTo(3);
    }
}
