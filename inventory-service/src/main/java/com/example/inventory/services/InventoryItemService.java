package com.example.inventory.services;

import com.example.inventory.dto.InventoryItemDetailsResponse;
import com.example.inventory.entities.InventoryItem;
import com.example.inventory.exceptions.NotFoundException;
import com.example.inventory.repositories.InventoryRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class InventoryItemService {
    private final InventoryRepository inventoryRepository;

    public InventoryItemService(InventoryRepository inventoryRepository){
        this.inventoryRepository = inventoryRepository;
    }

    public InventoryItemDetailsResponse getItem(Long id){
        Objects.requireNonNull(id);
        InventoryItem inventory = inventoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Item not found for id: " + id));
        return new InventoryItemDetailsResponse(
                inventory.getId(),
                inventory.getSku(),
                inventory.getTotal(),
                inventory.getAvailable()
        );
    }
}
