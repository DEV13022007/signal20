package com.example.sih26060.repository;

import com.example.sih26060.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByStation_Id(Long stationId);
}
