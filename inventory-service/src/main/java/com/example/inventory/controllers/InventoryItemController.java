package com.example.inventory.controllers;

import com.example.inventory.dto.CreateInventoryItemRequest;
import com.example.inventory.dto.InventoryItemDetailsResponse;
import com.example.inventory.dto.ItemResponse;
import com.example.inventory.dto.PaginatedItemResponse;
import com.example.inventory.services.InventoryItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/items")
public class InventoryItemController {
    private final InventoryItemService inventoryService;

    public InventoryItemController(InventoryItemService inventoryService){
        this.inventoryService = inventoryService;
    }


    @GetMapping("/{id}")
    public InventoryItemDetailsResponse getItem(@PathVariable Long id){
        return inventoryService.getItem(id);
    }

    @PostMapping
    public ResponseEntity<ItemResponse> createItem(@RequestBody @Valid CreateInventoryItemRequest request){
        ItemResponse itemResponse = inventoryService.createItem(request);
        URI location = URI.create("/items/" + itemResponse.id());
        return ResponseEntity.created(location).body(itemResponse);
    }

    @GetMapping
    public ResponseEntity<PaginatedItemResponse> getItems(Pageable pageable)
    {
        PaginatedItemResponse response = inventoryService.getPaginatedItems(pageable);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
