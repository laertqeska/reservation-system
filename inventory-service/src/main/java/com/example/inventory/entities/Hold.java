package com.example.inventory.entities;

import com.example.inventory.valueTypes.HoldStatus;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "holds")
public class Hold {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "hold_seq")
    @SequenceGenerator(name = "hold_seq",sequenceName = "hold_id_seq")
    private Long id;

    @Column(name = "item_id",nullable = false)
    private Long itemId;

    @Column(nullable = false)
    private int qty;

    @Column(name = "hold_key",length = 36,nullable = false,unique = true)
    private String holdKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HoldStatus status;

    @Column(name= "expires_at",nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at",nullable = false,updatable = false)
    private Instant createdAt;

    protected Hold(){}

    public Hold(Long itemId,int qty,String holdKey,Instant createdAt){
        Objects.requireNonNull(itemId);
        if(qty <= 0) throw new IllegalArgumentException("qty must be bigger than 0");
        if(holdKey.isBlank()) throw new IllegalArgumentException("holdKey cannot be blank");
        this.itemId = itemId;
        this.qty = qty;
        this.holdKey = holdKey;
        this.status = HoldStatus.HELD;
        this.createdAt = createdAt;
        this.expiresAt = createdAt.plus(Duration.ofMinutes(10));
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

    public String getHoldKey() {
        return holdKey;
    }

    public HoldStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
