package com.aifa.modules.importing.application.dto;

import com.aifa.modules.importing.domain.SmsImportStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SmsImportPreviewResponse(
        UUID batchId,
        SmsImportStatus status,
        String parserVersion,
        List<ParsedSmsRow> rows,
        Instant createdAt) {}
