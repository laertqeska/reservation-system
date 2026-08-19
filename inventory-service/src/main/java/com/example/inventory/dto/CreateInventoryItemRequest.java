package com.example.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateInventoryItemRequest(
        @NotBlank(message = "sku is required")
        String sku,
        @Min(value = 0,message = "total cannot be negative")
        int total,
        @Min(value = 0,message = "available cannot be negative")
        int available
) {
}
