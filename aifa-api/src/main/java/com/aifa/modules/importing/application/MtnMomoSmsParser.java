package com.aifa.modules.importing.application;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses MTN MoMo Rwanda SMS transaction messages (parser version mtn_momo_v2).
 *
 * <p>Supports multiple wording variants for payments, transfers, airtime, bank movements,
 * reversals, and salary/freelance inflows. Normalizes FRW to RWF and strips common prefixes.
 */
public class MtnMomoSmsParser {

    public static final String PARSER_VERSION = "mtn_momo_v2";
    private static final ZoneId KIGALI = ZoneId.of("Africa/Kigali");

    private static final Pattern AIRTEL_PATTERN = Pattern.compile("(?i)airtel\\s*money");
    private static final Pattern PROMO_ONLY_PATTERN =
            Pattern.compile("(?i)^Y'?ello!?\\s*Umaze\\s+kugura\\s+\\d+Frw");

    private static final Pattern BALANCE_PATTERN = Pattern.compile(
            "(?i)(?:your\\s+new\\s+balance\\s+is|remaining\\s+balance:?|your\\s+account\\s+balance\\s+is|available\\s+balance|balance\\s+is|balance:?)"
                    + "\\s*(?:(?:RWF\\s*)?([\\d,]+)|([\\d,]+)\\s*RWF)");

    private static final Pattern FEE_PATTERN = Pattern.compile("(?i)Fee:?\\s*([\\d,]+)\\s*RWF");

    private static final Pattern DATETIME_ISO_AT = Pattern.compile(
            "(?i)at\\s+(\\d{4})-(\\d{2})-(\\d{2})\\s+(\\d{2}):(\\d{2})(?::(\\d{2}))?");

    private static final Pattern DATETIME_ISO_BARE = Pattern.compile(
            "(\\d{4})-(\\d{2})-(\\d{2})\\s+(\\d{2}):(\\d{2})(?::(\\d{2}))?");

    private static final Pattern DATETIME_ON_AT = Pattern.compile(
            "(?i)on\\s+(\\d{1,2})/(\\d{1,2})/(\\d{4})\\s+at\\s+(\\d{1,2}):(\\d{2})");

    private static final Pattern DATETIME_ON = Pattern.compile(
            "(?i)on\\s+(\\d{1,2})/(\\d{1,2})/(\\d{4})\\s+(\\d{1,2}):(\\d{2})");

    private static final Pattern DATETIME_BARE =
            Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})\\s+(\\d{1,2}):(\\d{2})");

    private static final List<TransactionPattern> PATTERNS = List.of(
            // OUT — *165* style: "1500 RWF transferred to NAME (250...)"
            new TransactionPattern(
                    Pattern.compile(
                            "(?i)([\\d,]+)\\s+RWF\\s+transferred\\s+to\\s+(.+?)\\s*\\((\\d+)\\)"),
                    Direction.OUT,
                    true),
            // IN — "You have received 4700 RWF from NAME (phone/masked)"
            new TransactionPattern(
                    Pattern.compile(
                            "(?i)you\\s+have\\s+received\\s+([\\d,]+)\\s+RWF\\s+from\\s+(.+?)\\s*\\(([^)]+)\\)"),
                    Direction.IN,
                    true),
            // OUT — merchant debit: "A transaction of 1500 RWF by FIDOTECH was completed"
            new TransactionPattern(
                    Pattern.compile(
                            "(?i)a\\s+transaction\\s+of\\s+([\\d,]+)\\s+RWF\\s+by\\s+(.+?)\\s+was\\s+completed"),
                    Direction.OUT,
                    false),
            // IN — received with phone (RWF before amount)
            new TransactionPattern(
                    Pattern.compile(
                            "(?i)you\\s+have\\s+received\\s+(?:RWF\\s*)?([\\d,]+)\\s+from\\s+(.+?)\\s*\\((\\+?[\\d*]+)\\)"),
                    Direction.IN,
                    true),
            // IN — money received (compressed)
            new TransactionPattern(
                    Pattern.compile("(?i)money\\s+received\\s+(?:RWF\\s*)?([\\d,]+)\\s+from\\s+(.+?)(?:\\.|\\s+Balance)"),
                    Direction.IN,
                    false),
            // IN — received without requiring balance line
            new TransactionPattern(
                    Pattern.compile("(?i)you\\s+have\\s+received\\s+(?:RWF\\s*)?([\\d,]+)\\s+from\\s+(.+?)(?:\\.|\\s+on\\s+|\\s+Balance|\\s+Tx| at )"),
                    Direction.IN,
                    false),
            // IN — reversal / refund
            new TransactionPattern(
                    Pattern.compile("(?i)(?:reversal\\s+of|refund)\\s+(?:RWF\\s*)?([\\d,]+)(?:\\s+received)?(?:\\s+from\\s+(.+?))?(?:\\.|\\s+Balance)"),
                    Direction.IN,
                    false),
            // IN — salary / freelance
            new TransactionPattern(
                    Pattern.compile(
                            "(?i)(?:salary|freelance)\\s+payment\\s+(?:RWF\\s*)?([\\d,]+)\\s+received(?:\\s+from\\s+(.+?))?(?:\\.|\\s+Balance)"),
                    Direction.IN,
                    false),
            // IN — bank transfer in
            new TransactionPattern(
                    Pattern.compile(
                            "(?i)transfer\\s+from\\s+bank\\s+(.+?)\\.?\\s*amount\\s+(?:RWF\\s*)?([\\d,]+)"),
                    Direction.IN,
                    false,
                    true),
            // OUT — you have paid
            new TransactionPattern(
                    Pattern.compile("(?i)you\\s+have\\s+paid\\s+(?:RWF\\s*)?([\\d,]+)\\s+to\\s+(.+?)(?:\\.|\\s+Tx|\\s+Balance|\\s+on\\s+| at )"),
                    Direction.OUT,
                    false),
            // OUT — transferred / sent
            new TransactionPattern(
                    Pattern.compile(
                            "(?i)you\\s+have\\s+(?:transferred|sent)\\s+(?:RWF\\s*)?([\\d,]+)\\s+(?:to|for)\\s+(.+?)(?:\\.|\\s+on\\s+|\\s+Balance| at )"),
                    Direction.OUT,
                    false),
            // OUT — your payment of AMOUNT RWF to ... was completed (MoMo TxId / *162* / merchant)
            new TransactionPattern(
                    Pattern.compile(
                            "(?i)your\\s+payment\\s+of\\s+([\\d,]+)\\s+RWF\\s+to\\s+(.+?)\\s+was\\s+completed"),
                    Direction.OUT,
                    false),
            // OUT — payment of ... successful/completed
            new TransactionPattern(
                    Pattern.compile(
                            "(?i)payment\\s+of\\s+(?:RWF\\s*|FRW\\s*)?([\\d,]+)\\s+to\\s+(.+?)\\s+was\\s+(?:successful|completed)"),
                    Direction.OUT,
                    false),
            // OUT — payment of (generic)
            new TransactionPattern(
                    Pattern.compile("(?i)payment\\s+of\\s+(?:RWF\\s*|FRW\\s*)?([\\d,]+)\\s+to\\s+(.+?)(?:\\.|\\s+on\\s+|\\s+Balance|\\s+Ref| at )"),
                    Direction.OUT,
                    false),
            // OUT — merchant payment (RWF before amount)
            new TransactionPattern(
                    Pattern.compile(
                            "(?i)merchant\\s+payment\\s+(?:RWF\\s*)?([\\d,]+)(?:\\s+to\\s+(.+?))?(?:\\.|\\s+on\\s+|\\s+Ref|\\s+Balance)"),
                    Direction.OUT,
                    false),
            // OUT — merchant payment (amount before RWF)
            new TransactionPattern(
                    Pattern.compile("(?i)merchant\\s+payment\\s+([\\d,]+)\\s+RWF\\s+to\\s+(.+?)(?:\\.|\\s+Balance)"),
                    Direction.OUT,
                    false),
            // OUT — payment successful compressed
            new TransactionPattern(
                    Pattern.compile(
                            "(?i)payment\\s+successful\\.?\\s+(?:RWF\\s*)?([\\d,]+)\\s+paid\\s+to\\s+(.+?)(?:\\.|\\s+on\\s+|\\s+Tx|\\s+Balance)"),
                    Direction.OUT,
                    false),
            // OUT — amount RWF paid to (reversed order)
            new TransactionPattern(
                    Pattern.compile("(?i)([\\d,]+)\\s+RWF\\s+paid\\s+to\\s+(.+?)(?:\\.|\\s+Balance)"),
                    Direction.OUT,
                    false),
            // OUT — airtime / data bundle
            new TransactionPattern(
                    Pattern.compile(
                            "(?i)you\\s+bought\\s+(?:airtime|data\\s+bundle)\\s+(?:worth|of)\\s+(?:RWF\\s*)?([\\d,]+)(?:\\s+to\\s+(\\+?\\d[\\d\\s]*))?"),
                    Direction.OUT,
                    false),
            // OUT — bank transfer out
            new TransactionPattern(
                    Pattern.compile(
                            "(?i)transfer\\s+to\\s+bank\\s+(.+?)\\.?\\s*(?:amount\\s+)?(?:RWF\\s*)?([\\d,]+)"),
                    Direction.OUT,
                    false,
                    true));

    public ParseResult parse(String rawText, Instant defaultTimestamp) {
        if (AIRTEL_PATTERN.matcher(rawText).find()) {
            return ParseResult.airtelNotSupported();
        }

        String text = normalize(rawText);
        if (text.isBlank() || PROMO_ONLY_PATTERN.matcher(text).find()) {
            return ParseResult.unrecognized();
        }

        Long balance = extractBalance(text);
        Instant transactionAt = extractDateTime(text).orElse(defaultTimestamp);

        for (TransactionPattern pattern : PATTERNS) {
            Matcher matcher = pattern.pattern.matcher(text);
            if (!matcher.find()) {
                continue;
            }
            long amount = parseAmount(pattern.amountGroup(matcher));
            if (pattern.direction == Direction.OUT) {
                amount += extractFee(text);
            }
            String counterparty = pattern.counterparty(matcher);
            String phone = pattern.hasPhone ? normalizePhone(matcher.group(3)) : null;
            return ParseResult.success(amount, counterparty, phone, balance, transactionAt, pattern.direction.label);
        }

        return ParseResult.unrecognized();
    }

    String normalize(String text) {
        String normalized = text.trim();
        normalized = normalized.replaceFirst("^\\*\\d+\\*TxId:\\d+\\*S\\*", "");
        normalized = normalized.replaceFirst("^TxId:\\d+\\*S\\*", "");
        normalized = normalized.replaceFirst("^\\*\\d+\\*S\\*", "");
        normalized = normalized.replaceFirst("(?i)^(?:MTN\\s*MoMo\\s*:|Y'?ello[!:,]?\\s*)", "");
        normalized = normalized.replaceAll("(?i)\\bFRW\\b", "RWF");
        normalized = normalized.replaceAll("(?i)\\bFrw\\b", "RWF");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        normalized = normalized.replaceAll("(?i)\\s*Dial \\*\\d+.*$", "");
        normalized = normalized.replaceAll("(?i)\\s*\\*RW#.*$", "");
        normalized = normalized.replaceAll("(?i)\\s*\\*EN#.*$", "");
        return normalized;
    }

    private Long extractBalance(String text) {
        Matcher matcher = BALANCE_PATTERN.matcher(text);
        Long balance = null;
        while (matcher.find()) {
            String amount = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            balance = parseAmount(amount);
        }
        return balance;
    }

    private long extractFee(String text) {
        Matcher matcher = FEE_PATTERN.matcher(text);
        if (matcher.find()) {
            return parseAmount(matcher.group(1));
        }
        return 0L;
    }

    private java.util.Optional<Instant> extractDateTime(String text) {
        Matcher isoAt = DATETIME_ISO_AT.matcher(text);
        if (isoAt.find()) {
            return java.util.Optional.of(toInstantIso(isoAt));
        }
        Matcher isoBare = DATETIME_ISO_BARE.matcher(text);
        if (isoBare.find()) {
            return java.util.Optional.of(toInstantIso(isoBare));
        }
        Matcher onAt = DATETIME_ON_AT.matcher(text);
        if (onAt.find()) {
            return java.util.Optional.of(toInstantSlash(onAt));
        }
        Matcher on = DATETIME_ON.matcher(text);
        if (on.find()) {
            return java.util.Optional.of(toInstantSlash(on));
        }
        Matcher bare = DATETIME_BARE.matcher(text);
        if (bare.find()) {
            return java.util.Optional.of(toInstantSlash(bare));
        }
        return java.util.Optional.empty();
    }

    private Instant toInstantIso(Matcher matcher) {
        int year = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int day = Integer.parseInt(matcher.group(3));
        int hour = Integer.parseInt(matcher.group(4));
        int minute = Integer.parseInt(matcher.group(5));
        int second = matcher.group(6) != null ? Integer.parseInt(matcher.group(6)) : 0;
        return LocalDateTime.of(year, month, day, hour, minute, second).atZone(KIGALI).toInstant();
    }

    private Instant toInstantSlash(Matcher matcher) {
        int day = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int year = Integer.parseInt(matcher.group(3));
        int hour = Integer.parseInt(matcher.group(4));
        int minute = Integer.parseInt(matcher.group(5));
        return LocalDateTime.of(year, month, day, hour, minute).atZone(KIGALI).toInstant();
    }

    private long parseAmount(String raw) {
        return Long.parseLong(raw.replace(",", ""));
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.contains("*")) {
            return phone;
        }
        String digits = phone.replaceAll("\\s", "");
        if (digits.startsWith("0") && digits.length() == 10) {
            return "+25" + digits;
        }
        if (!digits.startsWith("+") && digits.startsWith("250") && digits.length() == 12) {
            return "+" + digits;
        }
        return digits;
    }

    private static String cleanCounterparty(String raw) {
        if (raw == null) {
            return null;
        }
        String name = raw.trim();
        name = name.replaceAll("(?i)\\s+with\\s+token\\s+.*$", "");
        name = name.replaceAll("(?i)\\s+and\\s+ET\\s+Id:.*$", "");
        name = name.replaceAll("(?i)\\s+(?:TxId:?|Ref:?|Reference:?|Transaction(?:\\s+ID)?:?|Transaction|FT\\s+Id|ET\\s+Id)\\s+.*$", "");
        name = name.replaceAll("\\s+", " ").trim();
        return name.isEmpty() ? null : name;
    }

    private enum Direction {
        IN("IN"),
        OUT("OUT");

        final String label;

        Direction(String label) {
            this.label = label;
        }
    }

    private static class TransactionPattern {
        final Pattern pattern;
        final Direction direction;
        final boolean hasPhone;
        final boolean bankSwap;

        TransactionPattern(Pattern pattern, Direction direction, boolean hasPhone) {
            this(pattern, direction, hasPhone, false);
        }

        TransactionPattern(Pattern pattern, Direction direction, boolean hasPhone, boolean bankSwap) {
            this.pattern = pattern;
            this.direction = direction;
            this.hasPhone = hasPhone;
            this.bankSwap = bankSwap;
        }

        String amountGroup(Matcher matcher) {
            return bankSwap ? matcher.group(2) : matcher.group(1);
        }

        String counterparty(Matcher matcher) {
            if (bankSwap) {
                return cleanCounterparty("Bank " + matcher.group(1).trim());
            }
            if (matcher.groupCount() >= 2 && matcher.group(2) != null) {
                return cleanCounterparty(matcher.group(2));
            }
            return null;
        }
    }

    public record ParseResult(
            boolean parsed,
            Long amountRwf,
            String counterpartyName,
            String phone,
            Long balanceRwf,
            Instant transactionAt,
            String direction,
            String parseError,
            String errorCode) {

        static ParseResult success(
                long amount,
                String counterparty,
                String phone,
                Long balance,
                Instant at,
                String direction) {
            return new ParseResult(true, amount, counterparty, phone, balance, at, direction, null, null);
        }

        static ParseResult unrecognized() {
            return new ParseResult(
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "SMS format not recognized by " + PARSER_VERSION,
                    com.aifa.shared.exception.ErrorCode.SMS_FORMAT_NOT_RECOGNIZED);
        }

        static ParseResult airtelNotSupported() {
            return new ParseResult(
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    com.aifa.shared.exception.ErrorCode.AIRTEL_NOT_SUPPORTED_FOR_MTN_WALLET,
                    com.aifa.shared.exception.ErrorCode.AIRTEL_NOT_SUPPORTED_FOR_MTN_WALLET);
        }
    }
}
