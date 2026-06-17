package com.aifa.modules.ledger.application.dto;

import jakarta.validation.constraints.Size;

public record UpdateWalletRequest(
        @Size(min = 1, max = 100) String name, Boolean primary, Long openingBalanceRwf) {}
