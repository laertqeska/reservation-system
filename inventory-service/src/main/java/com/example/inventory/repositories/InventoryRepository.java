package com.example.inventory.repositories;

import com.example.inventory.dto.InventoryItemModel;
import com.example.inventory.dto.ItemResponse;
import com.example.inventory.entities.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Modifying(clearAutomatically = true,flushAutomatically = true)
    @Query(value = """
        UPDATE inventory_items
            SET available = available + :qty,
                    updated_at = now()
        WHERE id = :id
    """,nativeQuery = true)
    int tryRelease(@Param("id") long id,@Param("qty") int qty);



    @Query("SELECT new com.example.inventory.dto.ItemResponse(item.id,item.sku,item.total,item.available,item.createdAt) " +
            "FROM InventoryItem item ")
    Page<ItemResponse> getPaginatedItems(Pageable pageable);
}
