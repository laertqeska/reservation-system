package com.example.reservation_service.dto;

import com.example.reservation_service.valueTypes.ReservationStatus;

import java.time.Instant;

public record ReservationResponse(
        Long id,
        Long itemId,
        int qty,
        Long holdId,
        ReservationStatus status,
        String failureReason,
        Instant createdAt
) {
}
