package com.example.inventory.dto;

import com.example.inventory.valueTypes.HoldStatus;

import java.time.Instant;

public record CreateHoldResponse(
        Long id,
        Long itemId,
        int qty,
        String holdKey,
        HoldStatus status,
        Instant createdAt,
        Instant expiresAt
) {
}
