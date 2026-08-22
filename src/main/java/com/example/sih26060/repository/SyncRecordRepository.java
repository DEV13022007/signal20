package com.example.sih26060.repository;

import com.example.sih26060.entity.SyncRecord;
import com.example.sih26060.entity.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SyncRecordRepository extends JpaRepository<SyncRecord, Long> {

    List<SyncRecord> findByStatus(SyncStatus status);

    List<SyncRecord> findByStation_IdAndStatus(Long stationId, SyncStatus status);

    long countByStatus(SyncStatus status);

    long countByStation_IdAndStatus(Long stationId, SyncStatus status);
}
