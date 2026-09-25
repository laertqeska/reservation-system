package com.example.reservation_service.dto;

public sealed interface CreateReservationResult permits CreateReservationResult.Held,CreateReservationResult.InsufficientStock,CreateReservationResult.ItemMissing,CreateReservationResult.Error{
    record Held(ReservationResponse response) implements CreateReservationResult {}
    record InsufficientStock(ReservationResponse response) implements CreateReservationResult {}
    record ItemMissing(ReservationResponse response) implements CreateReservationResult {}
    record Error(ReservationResponse response,int status) implements CreateReservationResult {}
}
