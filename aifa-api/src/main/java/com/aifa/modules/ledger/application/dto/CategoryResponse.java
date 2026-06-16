package com.aifa.modules.ledger.application.dto;

import java.util.UUID;

public record CategoryResponse(UUID id, String name, String slug, String icon, boolean system) {}
