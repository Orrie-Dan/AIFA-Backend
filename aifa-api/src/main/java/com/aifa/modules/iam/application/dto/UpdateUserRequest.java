package com.aifa.modules.iam.application.dto;

import com.aifa.modules.iam.domain.AiMode;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(@NotNull AiMode aiMode) {}
