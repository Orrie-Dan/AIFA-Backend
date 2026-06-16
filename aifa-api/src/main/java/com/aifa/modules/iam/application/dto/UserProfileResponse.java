package com.aifa.modules.iam.application.dto;

import com.aifa.modules.iam.domain.AiMode;
import java.util.UUID;

public record UserProfileResponse(UUID userId, String email, AiMode aiMode) {}
