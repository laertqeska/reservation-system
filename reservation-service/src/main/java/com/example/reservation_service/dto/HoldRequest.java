package com.example.reservation_service.dto;

public record HoldRequest(
        Long itemId,
        int qty,
        String holdKey
) {
}
