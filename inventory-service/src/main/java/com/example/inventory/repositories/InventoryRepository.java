package com.example.inventory.repositories;

import com.example.inventory.entities.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryItem,Long> {
    Optional<InventoryItem> findById(Long id);
}
