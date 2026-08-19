package com.example.inventory.services;

import com.example.inventory.dto.CreateInventoryItemRequest;
import com.example.inventory.dto.InventoryItemDetailsResponse;
import com.example.inventory.dto.ItemResponse;
import com.example.inventory.entities.InventoryItem;
import com.example.inventory.exceptions.InventoryItemAlreadyExistsException;
import com.example.inventory.exceptions.NotFoundException;
import com.example.inventory.repositories.InventoryRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
public class InventoryItemService {
    private final InventoryRepository inventoryRepository;
    private final Clock clock;

    public InventoryItemService(InventoryRepository inventoryRepository, Clock clock){
        this.inventoryRepository = inventoryRepository;
        this.clock = clock;
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

    public ItemResponse createItem(CreateInventoryItemRequest request){
        Objects.requireNonNull(request);
        boolean existsBySku = inventoryRepository.existsBySku(request.sku());

        if(existsBySku){
            throw new InventoryItemAlreadyExistsException(request.sku());
        }

        InventoryItem inventoryItem = new InventoryItem(
                request.sku(),
                request.total(),
                request.available(),
                Instant.now(clock)
        );

        inventoryItem = inventoryRepository.save(inventoryItem);

        return new ItemResponse(
                inventoryItem.getId(),
                inventoryItem.getSku(),
                inventoryItem.getTotal(),
                inventoryItem.getAvailable(),
                inventoryItem.getCreatedAt()
        );
    }
}
