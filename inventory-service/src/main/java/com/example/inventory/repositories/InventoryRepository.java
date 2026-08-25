package com.example.inventory.repositories;

import com.example.inventory.entities.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<InventoryItem,Long> {
    boolean existsBySku(String sku);


    @Modifying(clearAutomatically = true,flushAutomatically = true)
    @Query(value = """
        UPDATE inventory_items
            SET available = available - :qty,
                updated_at = now()
        WHERE id = :id
            AND available >= :qty
    """,nativeQuery = true)
    int tryReserve(@Param("id") long id,@Param("qty") int qty);
}
