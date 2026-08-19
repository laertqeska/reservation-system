package com.example.inventory.repositories;

import com.example.inventory.entities.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryItem,Long> {
    boolean existsBySku(String sku);
}
