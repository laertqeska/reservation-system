package com.example.reservation_service.exceptions;

public class InventoryItemNotFoundException extends RuntimeException {
    private final Long itemId;
    public InventoryItemNotFoundException(Long itemId) {
        super("Item " + itemId + " does not exist in the inventory");
        this.itemId = itemId;
    }
}
