package com.aifa.modules.importing.application.dto;

import java.util.List;
import java.util.UUID;

public record SmsImportConfirmResponse(UUID batchId, int importedCount, List<UUID> transactionIds) {}
