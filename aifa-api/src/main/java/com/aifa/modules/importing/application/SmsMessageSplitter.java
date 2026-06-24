package com.aifa.modules.importing.application;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits a pasted SMS export into individual messages.
 *
 * <p>Blank lines always separate messages. When lines are pasted back-to-back (common from phone
 * exports), a new line that looks like a known MTN MoMo message start also begins a new message.
 * Wrapped multi-line SMS without a starter on continuation lines are kept together.
 */
final class SmsMessageSplitter {

    private static final Pattern MESSAGE_START = Pattern.compile(
            "(?i)^("
                    + "\\*\\d+\\*"
                    + "|MTN MoMo:"
                    + "|Y'?ello!?"
                    + "|You have (received|transferred|sent|paid)\\b"
                    + "|Payment (of|successful)\\b"
                    + "|Merchant payment\\b"
                    + "|You bought\\b"
                    + "|Transfer (to|from) Bank\\b"
                    + "|Money received\\b"
                    + "|\\d[\\d,]*\\s+RWF\\s+(paid|transferred)\\b"
                    + "|Reversal of\\b"
                    + "|Refund\\b"
                    + "|Salary payment\\b"
                    + "|Freelance payment\\b"
                    + "|Airtel Money:"
                    + "|Your Airtel Money\\b"
                    + "|TxId:"
                    + "|Your airtime bundle\\b"
                    + "|Welcome to MTN\\b"
                    + ")");

    private SmsMessageSplitter() {}

    static List<String> split(String rawMessages) {
        String[] lines = rawMessages.split("\\R");
        List<String> messages = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        for (String line : lines) {
            if (line.isBlank()) {
                flush(buffer, messages);
                continue;
            }
            String trimmed = line.trim();
            if (!buffer.isEmpty() && looksLikeMessageStart(trimmed)) {
                flush(buffer, messages);
            }
            if (!buffer.isEmpty()) {
                buffer.append(' ');
            }
            buffer.append(trimmed);
        }
        flush(buffer, messages);
        return messages;
    }

    static boolean looksLikeMessageStart(String line) {
        return MESSAGE_START.matcher(line).lookingAt();
    }

    private static void flush(StringBuilder buffer, List<String> messages) {
        if (!buffer.isEmpty()) {
            messages.add(buffer.toString().trim());
            buffer.setLength(0);
        }
    }
}
