package com.example.sih26060.repository;

import com.example.sih26060.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findByStation_Id(Long stationId);
}
