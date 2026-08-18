package com.example.inventory.dto;

import java.util.Objects;

public record InventoryItemDetailsResponse(
        Long id,
        String sku,
        int total,
        int available
) {
    public InventoryItemDetailsResponse {
        Objects.requireNonNull(id);
        Objects.requireNonNull(sku);
        if(sku.isBlank()) throw new IllegalArgumentException("sku cannot be blank");
        if(total < 0) throw new IllegalArgumentException("total cannot be negative");
        if(available < 0) throw new IllegalArgumentException("available cannot be negative");
    }
}
