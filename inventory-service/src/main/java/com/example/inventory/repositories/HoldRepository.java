package com.example.inventory.repositories;

import com.example.inventory.entities.Hold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HoldRepository extends JpaRepository<Hold,Long> {
    Optional<Hold> findByHoldKey(String holdKey);

    @Modifying(clearAutomatically = true,flushAutomatically = true)
    @Query(value = """
        UPDATE hold
            SET status = 'COMMITTED',updated_at = now()
        WHERE id = :id AND status = 'HELD'
    """,nativeQuery = true)
    int tryCommit(@Param("id") long id);


    @Modifying(clearAutomatically = true,flushAutomatically = true)
    @Query(value = """
        UPDATE hold
            SET status = 'RELEASED', updated_at = now()
        WHERE id = :id AND status = 'HELD'

    """,nativeQuery = true)
    int tryRelease(@Param("id") long id);
}
