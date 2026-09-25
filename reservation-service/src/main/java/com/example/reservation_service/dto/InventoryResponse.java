package com.example.reservation_service.dto;

import java.time.Instant;

public record InventoryResponse(
        Long id,
        Long itemId,
        int qty,
        String holdKey,
        String status,
        Instant createdAt,
        Instant expiresAt
) {
}
