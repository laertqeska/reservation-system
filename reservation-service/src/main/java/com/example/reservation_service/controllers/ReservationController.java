package com.example.reservation_service.controllers;

import com.example.reservation_service.dto.CreateReservationRequest;
import com.example.reservation_service.dto.ReservationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.reservation_service.services.ReservationService;

@RestController
@RequestMapping("/reservations")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid CreateReservationRequest request
            ) {
        ReservationResponse response = reservationService.createReservation(request,idempotencyKey);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable Long id){
        ReservationResponse response = reservationService.getReservation(id);
        return new ResponseEntity<>(response,HttpStatus.OK);
    }
}
