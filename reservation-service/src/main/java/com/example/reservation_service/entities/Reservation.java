package com.example.reservation_service.entities;

import jakarta.persistence.*;
import com.example.reservation_service.valueTypes.ReservationStatus;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "reservation")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "reservation_seq")
    @SequenceGenerator(name = "reservation_seq",sequenceName="reservation_id_seq")
    private Long id;

    @Column(name = "item_id",nullable = false)
    private Long itemId;

    @Column(nullable = false)
    private int qty;

    @Column(name = "idempotency_key",nullable = false)
    private String idempotencyKey;

    @Column(name = "hold_id")
    private Long holdId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at",nullable = false,updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at",nullable = false)
    private Instant updatedAt;


    protected Reservation(){}

    public Reservation(Long itemId,int qty,String idempotencyKey){
        Objects.requireNonNull(itemId);
        if(qty <= 0){
            throw new IllegalArgumentException("qty cannot be negative or zero");
        }
        Objects.requireNonNull(idempotencyKey);
        this.itemId = itemId;
        this.idempotencyKey = idempotencyKey;
        this.qty = qty;
        status = ReservationStatus.PENDING;
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getItemId() {
        return itemId;
    }

    public int getQty() {
        return qty;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getHoldId() {
        return holdId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markHeld(Long holdId,Instant now){
        if(this.status != ReservationStatus.PENDING){
            throw new IllegalStateException("can only held a PENDING reservation");
        }
        Objects.requireNonNull(holdId,"holdId cannot be null");
        this.holdId = holdId;
        this.status = ReservationStatus.HELD;
        updatedAt = now;
    }

    public void markFailed(String reason,Instant now){
        if(this.status != ReservationStatus.PENDING){
            throw new IllegalStateException("can only fail a PENDING reservation");
        }
        Objects.requireNonNull(reason,"reason cannot be null");
        this.failureReason = reason;
        this.status = ReservationStatus.FAILED;
        this.updatedAt = now;
    }
}
