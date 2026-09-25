package com.example.reservation_service.exceptions;

public class InsufficientInventoryException extends RuntimeException {
    private final Long itemId;
    private final int requestedQty;

  public InsufficientInventoryException(Long itemId, int requestedQty) {
    super("Item " + itemId + " does not have " + requestedQty + " units available");
    this.itemId = itemId;
    this.requestedQty = requestedQty;
  }

  public Long getItemId() {
    return itemId;
  }

  public int getRequestedQty() {
    return requestedQty;
  }
}
