package com.example.sih26060.repository;

import com.example.sih26060.entity.HealthStatus;
import com.example.sih26060.entity.Personnel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonnelRepository extends JpaRepository<Personnel, Long> {

    List<Personnel> findByStation_Id(Long stationId);

    List<Personnel> findByHealthStatus(HealthStatus healthStatus);
}
