package com.example.inventory.dto;

import java.time.Instant;

public record InventoryItemModel(
        Long id,
        String sku,
        int total,
        int available,
        Instant createdAt
) {

}
