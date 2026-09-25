package com.example.reservation_service.dto;

public sealed interface HoldOutcome permits HoldOutcome.Success,HoldOutcome.InsufficientStock,HoldOutcome.ItemNotFound,HoldOutcome.Error{
    record Success(InventoryResponse response) implements HoldOutcome {}
    record InsufficientStock() implements HoldOutcome {}
    record ItemNotFound() implements HoldOutcome {}
    record Error(int status,String error) implements HoldOutcome {}
}
