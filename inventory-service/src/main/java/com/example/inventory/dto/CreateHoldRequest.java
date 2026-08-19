package com.example.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateHoldRequest(
        @NotNull(message = "itemId cannot be null")
        Long itemId,
        @Min(value = 1,message = "qty cannot be zero or negative")
        int qty,
        @Size(min=36,max=36,message = "holdKey must be a 36-characteer UUID string")
        String holdKey
) {
}
