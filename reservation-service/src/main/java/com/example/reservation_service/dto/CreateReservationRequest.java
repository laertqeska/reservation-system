package com.example.reservation_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateReservationRequest(
        @NotNull(message = "itemId cannot be null")
        Long itemId,
        @Min(value = 1,message = "qty cannot be zero or negative")
        int qty
) {
}
