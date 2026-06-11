package com.example.backend.repository;

import com.example.backend.model.Inventory;
import com.example.backend.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByVariant(ProductVariant variant);
}