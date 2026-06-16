package com.aifa.modules.importing.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record SmsImportConfirmRequest(
        @NotNull UUID batchId, @NotEmpty List<Integer> rowIndexes, UUID categoryId) {}
