package com.example.inventory.exceptions;

public class HoldHasBeenReleasedException extends RuntimeException {
    public HoldHasBeenReleasedException(String message) {
        super(message);
    }
}
