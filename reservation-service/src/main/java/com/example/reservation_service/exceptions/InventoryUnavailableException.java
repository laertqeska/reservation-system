package com.example.reservation_service.exceptions;

public class InventoryUnavailableException extends RuntimeException {
    private final int upstreamStatus;

    public InventoryUnavailableException(int upstreamStatus) {
        super("Inventory service returned " + upstreamStatus);
        this.upstreamStatus = upstreamStatus;
    }

    public int getUpstreamStatus(){
      return upstreamStatus;
    }
}
