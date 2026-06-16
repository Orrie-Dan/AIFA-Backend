package com.aifa.modules.importing.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record SmsImportPreviewRequest(@NotBlank String messages, UUID walletId) {}
