package com.example.reservation_service.services;

import com.example.reservation_service.dto.*;
import com.example.reservation_service.entities.Reservation;
import com.example.reservation_service.exceptions.InventoryUnavailableException;
import com.example.reservation_service.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.example.reservation_service.repositories.ReservationRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RestClient restClient;
    private final Clock clock;


    public ReservationService(ReservationRepository reservationRepository, RestClient.Builder builder, @Value("${inventory.base-url}") String inventoryBaseUrl, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.clock = clock;
        this.restClient = builder.baseUrl(inventoryBaseUrl).build();
    }

    public ReservationResponse createReservation(CreateReservationRequest request,String idempotencyKey){
        Objects.requireNonNull(request);
        Optional<Reservation> existingReservation = reservationRepository.findByIdempotencyKey(idempotencyKey);
        if(existingReservation.isPresent()){
            return toResponse(existingReservation.get());
        }
        Reservation reservation = new Reservation(
                request.itemId(),
                request.qty(),
                idempotencyKey
        );
        reservationRepository.save(reservation);
        HoldOutcome holdOutcome = restClient.post()
                .uri("/holds")
                .body(new HoldRequest(
                        request.itemId(),
                        request.qty(),
                        idempotencyKey
                ))
                .exchange((req,res) -> {
                            int status = res.getStatusCode().value();
                            if(status == 201){
                                return new HoldOutcome.Success(res.bodyTo(InventoryResponse.class));
                            }
                            if(status == 409){
                                return new HoldOutcome.InsufficientStock();
                            }
                            if(status == 404){
                                return new HoldOutcome.ItemNotFound();
                            }
                            return new HoldOutcome.Error(status);
                });

        return switch (holdOutcome) {
            case HoldOutcome.Success s -> {
                reservation.markHeld(s.response().id(), Instant.now(clock));
                reservationRepository.save(reservation);
                yield toResponse(reservation);
            }
            case HoldOutcome.InsufficientStock ignored -> {
                reservation.markFailed("INSUFFICIENT_INVENTORY", Instant.now(clock));
                reservationRepository.save(reservation);
                yield toResponse(reservation);
            }
            case HoldOutcome.ItemNotFound ignored -> {
                reservation.markFailed("ITEM_NOT_FOUND", Instant.now(clock));
                reservationRepository.save(reservation);
                yield toResponse(reservation);
            }
            case HoldOutcome.Error e -> {
                reservation.markFailed("INVENTORY_ERROR_" + e.status(), Instant.now(clock));
                reservationRepository.save(reservation);
                throw new InventoryUnavailableException(e.status());
            }
        };
    }

    public ReservationResponse getReservation(Long id){
        Reservation reservation =  this.reservationRepository.findById(id).orElseThrow(() -> new NotFoundException("Reservation not found for id: " + id));
        return toResponse(reservation);
    }

    private ReservationResponse toResponse(Reservation reservation){
        return new ReservationResponse(
                reservation.getId(),
                reservation.getItemId(),
                reservation.getQty(),
                reservation.getHoldId(),
                reservation.getStatus(),
                reservation.getFailureReason(),
                reservation.getCreatedAt()
        );
    }


}
