package com.aifa.modules.importing.application;

import com.aifa.modules.importing.application.MtnMomoSmsParser.ParseResult;
import com.aifa.modules.importing.application.dto.ParsedSmsRow;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SmsParseService {

    public static final String PARSER_VERSION = MtnMomoSmsParser.PARSER_VERSION;

    private final MtnMomoSmsParser mtnMomoSmsParser = new MtnMomoSmsParser();

    public List<ParsedSmsRow> parseBatch(String rawMessages, Instant defaultTimestamp) {
        List<String> messages = SmsMessageSplitter.split(rawMessages);
        List<ParsedSmsRow> rows = new ArrayList<>();
        for (int index = 0; index < messages.size(); index++) {
            rows.add(parseSingle(messages.get(index), index, defaultTimestamp));
        }
        return rows;
    }

    ParsedSmsRow parseSingle(String text, int rowIndex, Instant defaultTimestamp) {
        ParseResult result = mtnMomoSmsParser.parse(text, defaultTimestamp);
        if (!result.parsed()) {
            return ParsedSmsRow.failure(rowIndex, text, result.parseError());
        }
        return ParsedSmsRow.success(
                rowIndex,
                text,
                result.amountRwf(),
                result.counterpartyName(),
                result.phone(),
                result.balanceRwf(),
                result.transactionAt(),
                result.direction());
    }
}
