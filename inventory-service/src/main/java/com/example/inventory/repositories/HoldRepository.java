package com.example.inventory.repositories;

import com.example.inventory.entities.Hold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HoldRepository extends JpaRepository<Hold,Long> {
    Optional<Hold> findByHoldKey(String holdKey);
}
