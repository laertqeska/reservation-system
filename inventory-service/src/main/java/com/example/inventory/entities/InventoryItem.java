package com.example.inventory.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;


@Entity
@Table(name = "inventory_items")
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "inventory_seq")
    @SequenceGenerator(name = "inventory_seq",sequenceName="inventory_item_id_seq")
    private Long id;

    @Column(updatable = false,nullable = false,unique = true,length = 50)
    private String sku;

    @Column(nullable = false)
    private int total;

    @Column(nullable = false)
    private int available;

    @Version
    private Long version;

    @Column(name = "created_at",nullable = false,updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at",nullable = false)
    private Instant updatedAt;

    protected InventoryItem(){}

    public InventoryItem(String sku, int total, int available, Instant createdAt){
        Objects.requireNonNull(sku);
        if(sku.isBlank()) throw new IllegalArgumentException("sku cannot be blank");
        if(total < 0) throw new IllegalArgumentException("total cannot be negative");
        if(available < 0) throw new IllegalArgumentException("available cannot be negative");
        this.sku = sku;
        this.total = total;
        this.available = available;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public int getTotal() {
        return total;
    }

    public int getAvailable() {
        return available;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
