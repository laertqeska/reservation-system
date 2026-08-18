package com.example.inventory.controllers;

import com.example.inventory.dto.InventoryItemDetailsResponse;
import com.example.inventory.services.InventoryItemService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
