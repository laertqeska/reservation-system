package com.example.inventory.exceptions;

public class InventoryItemAlreadyExistsException extends RuntimeException {
    public InventoryItemAlreadyExistsException(String sku) {
        super("Inventory item with sku: " + sku + " already exists");
    }
}
