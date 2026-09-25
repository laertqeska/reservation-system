package com.example.reservation_service.repositories;

import com.example.reservation_service.entities.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation,Long> {
    Optional<Reservation> findByIdempotencyKey(String idempotencyKey);
}
