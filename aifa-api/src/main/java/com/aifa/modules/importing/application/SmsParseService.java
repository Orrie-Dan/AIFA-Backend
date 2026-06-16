package com.aifa.modules.importing.application;

import com.aifa.modules.iam.application.AuthService;
import com.aifa.modules.importing.application.dto.ParsedSmsRow;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class SmsParseService {

    public static final String PARSER_VERSION = "mtn_momo_v1";

    private static final Pattern RECEIVED_PATTERN = Pattern.compile(
            "(?i)You have received RWF\\s*([\\d,]+)\\s+from\\s+(.+?)\\s*\\((\\+?\\d+)\\)\\.?\\s*Your new balance is RWF\\s*([\\d,]+)",
            Pattern.DOTALL);

    private static final Pattern SENT_PATTERN = Pattern.compile(
            "(?i)(?:You have (?:transferred|sent)|Payment of) RWF\\s*([\\d,]+)\\s+(?:to|for)\\s+(.+?)(?:\\.|\\s+Your new balance is RWF\\s*([\\d,]+))",
            Pattern.DOTALL);

    public List<ParsedSmsRow> parseBatch(String rawMessages, Instant defaultTimestamp) {
        String[] lines = rawMessages.split("\\R");
        List<ParsedSmsRow> rows = new ArrayList<>();
        int index = 0;
        StringBuilder buffer = new StringBuilder();

        for (String line : lines) {
            if (line.isBlank()) {
                if (!buffer.isEmpty()) {
                    rows.add(parseSingle(buffer.toString().trim(), index++, defaultTimestamp));
                    buffer.setLength(0);
                }
                continue;
            }
            if (!buffer.isEmpty()) {
                buffer.append(' ');
            }
            buffer.append(line.trim());
        }
        if (!buffer.isEmpty()) {
            rows.add(parseSingle(buffer.toString().trim(), index, defaultTimestamp));
        }
        return rows;
    }

    ParsedSmsRow parseSingle(String text, int rowIndex, Instant defaultTimestamp) {
        Matcher received = RECEIVED_PATTERN.matcher(text);
        if (received.find()) {
            long amount = parseAmount(received.group(1));
            String sender = received.group(2).trim();
            String phone = received.group(3).trim();
            long balance = parseAmount(received.group(4));
            return ParsedSmsRow.success(
                    rowIndex, text, amount, sender, phone, balance, defaultTimestamp, "credit");
        }

        Matcher sent = SENT_PATTERN.matcher(text);
        if (sent.find()) {
            long amount = parseAmount(sent.group(1));
            String recipient = sent.group(2).trim();
            Long balance = sent.group(3) != null ? parseAmount(sent.group(3)) : null;
            return ParsedSmsRow.success(rowIndex, text, amount, recipient, null, balance, defaultTimestamp, "debit");
        }

        return ParsedSmsRow.failure(rowIndex, text, "SMS format not recognized by " + PARSER_VERSION);
    }

    private long parseAmount(String raw) {
        return Long.parseLong(raw.replace(",", ""));
    }

    String hashPhone(String phone) {
        return AuthService.hashValue(phone);
    }
}
