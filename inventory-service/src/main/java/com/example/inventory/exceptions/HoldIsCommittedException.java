package com.example.inventory.exceptions;

public class HoldIsCommittedException extends RuntimeException {
    public HoldIsCommittedException(String message) {
        super(message);
    }
}
