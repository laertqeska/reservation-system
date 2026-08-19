package com.example.inventory.services;

import com.example.inventory.dto.CreateHoldRequest;
import com.example.inventory.dto.CreateHoldResponse;
import com.example.inventory.entities.Hold;
import com.example.inventory.entities.InventoryItem;
import com.example.inventory.exceptions.NotFoundException;
import com.example.inventory.repositories.HoldRepository;
import com.example.inventory.repositories.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
public class HoldService {
    private final HoldRepository holdRepository;
    private final InventoryRepository inventoryRepository;
    private final Clock clock;

    public HoldService(HoldRepository holdRepository, InventoryRepository inventoryRepository, Clock clock) {
        this.holdRepository = holdRepository;
        this.inventoryRepository = inventoryRepository;
        this.clock = clock;
    }

    @Transactional
    public CreateHoldResponse createHold(CreateHoldRequest request){
        Objects.requireNonNull(request);
        Optional<Hold> existingHold = holdRepository.findByHoldKey(request.holdKey());
        if(existingHold.isPresent()){
            return toResponse(existingHold.get());
        }
        InventoryItem item = inventoryRepository.findById(request.itemId()).orElseThrow(() -> new NotFoundException("Inventory item not found for id: " + request.itemId()));
        item.hold(request.qty(),clock);
        inventoryRepository.save(item);
        Hold hold = new Hold(
                request.itemId(),request.qty(),request.holdKey(), Instant.now(clock)
        );
        hold = holdRepository.save(hold);
        return toResponse(hold);
    }

    private CreateHoldResponse toResponse(Hold hold) {
        return new CreateHoldResponse(
                hold.getId(),
                hold.getItemId(),
                hold.getQty(),
                hold.getHoldKey(),
                hold.getStatus(),
                hold.getCreatedAt(),
                hold.getExpiresAt()
        );
    }
}
