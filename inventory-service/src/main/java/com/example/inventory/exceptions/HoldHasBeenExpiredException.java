package com.example.inventory.exceptions;

public class HoldHasBeenExpiredException extends RuntimeException {
    public HoldHasBeenExpiredException(String message) {
        super(message);
    }
}
