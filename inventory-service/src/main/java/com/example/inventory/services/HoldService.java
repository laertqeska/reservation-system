package com.example.inventory.services;

import com.example.inventory.dto.CreateHoldRequest;
import com.example.inventory.dto.HoldResponse;
import com.example.inventory.entities.Hold;
import com.example.inventory.exceptions.*;
import com.example.inventory.repositories.HoldRepository;
import com.example.inventory.repositories.InventoryRepository;
import com.example.inventory.valueTypes.HoldStatus;
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
    public HoldResponse createHold(CreateHoldRequest request){
        Objects.requireNonNull(request);
        Optional<Hold> existingHold = holdRepository.findByHoldKey(request.holdKey());
        if(existingHold.isPresent()){
            return toResponse(existingHold.get());
        }
        if(!inventoryRepository.existsById(request.itemId())){
            throw new NotFoundException("Inventory item not found for id: " + request.itemId());
        }
        int affected = inventoryRepository.tryReserve(request.itemId(),request.qty());
        if(affected == 0){
            throw new InsufficientInventoryException("Not enough available stock");
        }
        Hold hold = new Hold(
                request.itemId(),request.qty(),request.holdKey(), Instant.now(clock)
        );
        hold = holdRepository.save(hold);
        return toResponse(hold);
    }

    private HoldResponse toResponse(Hold hold) {
        return new HoldResponse(
                hold.getId(),
                hold.getItemId(),
                hold.getQty(),
                hold.getHoldKey(),
                hold.getStatus(),
                hold.getCreatedAt(),
                hold.getExpiresAt()
        );
    }

    @Transactional
    public HoldResponse commitHold(Long id){
        Objects.requireNonNull(id,"Id cannot be null");
        Hold hold;
        int affected = holdRepository.tryCommit(id);
        if(affected == 0){
            hold = holdRepository.findById(id).orElseThrow(() -> new NotFoundException("Hold not found for id: " + id));
            switch(hold.getStatus()){
                case HoldStatus.COMMITTED -> {
                    return toResponse(hold);
                }
                case HoldStatus.RELEASED -> throw new HoldHasBeenReleasedException("Hold is released");
                case HoldStatus.EXPIRED -> throw new HoldHasBeenExpiredException("Hold is expired");
            }
        }
        hold = holdRepository.findById(id).orElseThrow(() -> new NotFoundException("Hold not found for id: " + id));
        return toResponse(hold);
    }

    @Transactional
    public HoldResponse releaseHold(Long id){
        Objects.requireNonNull(id);
        Hold hold;
        int affected = holdRepository.tryRelease(id);
        hold = holdRepository.findById(id).orElseThrow(() -> new NotFoundException("Hold not found for id: " + id));
        if(affected == 0){
            switch(hold.getStatus()){
                case RELEASED -> throw new HoldHasBeenReleasedException("Hold has already been released");
                case COMMITTED -> throw new HoldIsCommittedException("Hold is commited");
                case EXPIRED -> throw new HoldHasBeenExpiredException("Hold is expired");
            }
        }

        int inventoryAffected = inventoryRepository.tryRelease(hold.getItemId(),hold.getQty());
        if(inventoryAffected == 0){
            throw new NotFoundException("Inventory item not found for id: " + hold.getItemId());
        }
        return toResponse(hold);
    }


}
