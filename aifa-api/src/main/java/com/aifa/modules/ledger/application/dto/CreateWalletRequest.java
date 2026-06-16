package com.aifa.modules.ledger.application.dto;

import com.aifa.modules.ledger.domain.WalletType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWalletRequest(
        @NotBlank String name, @NotNull WalletType type, boolean primary) {}
