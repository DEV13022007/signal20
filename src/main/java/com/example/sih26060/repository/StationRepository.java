package com.example.sih26060.repository;

import com.example.sih26060.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StationRepository extends JpaRepository<Station, Long> {

    List<Station> findBySatelliteLinkActiveTrue();
}
