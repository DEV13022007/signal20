package com.example.sih26060.seed;

import com.example.sih26060.entity.InventoryItem;
import com.example.sih26060.entity.Priority;
import com.example.sih26060.entity.Season;
import com.example.sih26060.entity.Station;
import com.example.sih26060.repository.InventoryItemRepository;
import com.example.sih26060.repository.StationRepository;
import com.example.sih26060.repository.SyncRecordRepository;
import com.example.sih26060.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Demo/hackathon seed data: wipes and re-inserts Maitri + Bharati (with inventory) on every
 * boot so restarting the backend is the reset button for the dashboard demo. Disable via
 * polarconnect.seed.enabled=false once this stops being a throwaway prototype.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final StationRepository stationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SyncRecordRepository syncRecordRepository;
    private final InventoryService inventoryService;

    @Value("${polarconnect.seed.enabled:true}")
    private boolean enabled;

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        syncRecordRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        stationRepository.deleteAll();

        LocalDate today = LocalDate.now();

        Station maitri = stationRepository.save(Station.builder()
                .code("MAITRI")
                .name("Maitri Station")
                .country("India")
                .latitude(-70.76)
                .longitude(11.73)
                .capacity(25)
                .currentSeason(Season.WINTER)
                .operationalSinceYear(1989)
                .satelliteLinkActive(true)
                .build());

        Station bharati = stationRepository.save(Station.builder()
                .code("BHARATI")
                .name("Bharati Station")
                .country("India")
                .latitude(-69.40)
                .longitude(76.19)
                .capacity(47)
                .currentSeason(Season.SUMMER)
                .operationalSinceYear(2012)
                .satelliteLinkActive(false)
                .build());

        // Maitri: link is already up, so every item created here is enqueued and immediately
        // flushed by InventoryService/SyncQueueService — this station shows fully SYNCED.
        seedItem(maitri, "Insulin", Priority.MEDICAL, 2, "vials", 10, today.plusDays(45));
        seedItem(maitri, "Antibiotics", Priority.MEDICAL, 15, "boxes", 8, today.minusDays(30));
        seedItem(maitri, "Bandages", Priority.MEDICAL, 50, "packs", 20, today.plusDays(300));
        seedItem(maitri, "Generator Part", Priority.EQUIPMENT, 3, "units", 1, null);
        seedItem(maitri, "Snowmobile Battery", Priority.EQUIPMENT, 1, "units", 2, null);
        seedItem(maitri, "Rice", Priority.SUPPLY, 180, "kg", 50, today.plusDays(55));
        seedItem(maitri, "Powdered Milk", Priority.SUPPLY, 40, "kg", 50, today.plusDays(20));
        seedItem(maitri, "Diesel Fuel", Priority.SUPPLY, 500, "litres", 100, null);
        seedItem(maitri, "Notebooks & Stationery", Priority.ROUTINE, 25, "units", 5, null);
        seedItem(maitri, "Cold-Weather Clothing", Priority.ROUTINE, 40, "units", 10, null);

        // Bharati: link is down, so all 8 items stay PENDING and demonstrate the
        // priority-ordered sync queue (MEDICAL > EQUIPMENT > SUPPLY > ROUTINE).
        seedItem(bharati, "Painkillers", Priority.MEDICAL, 30, "boxes", 10, today.plusDays(86));
        seedItem(bharati, "First Aid Kits", Priority.MEDICAL, 12, "kits", 4, today.plusDays(560));
        seedItem(bharati, "Weather Sensor", Priority.EQUIPMENT, 6, "units", 2, null);
        seedItem(bharati, "Satellite Radio Unit", Priority.EQUIPMENT, 4, "units", 2, null);
        seedItem(bharati, "Rice", Priority.SUPPLY, 220, "kg", 60, today.plusDays(400));
        seedItem(bharati, "Canned Vegetables", Priority.SUPPLY, 150, "kg", 40, today.plusDays(121));
        seedItem(bharati, "Stationery Kits", Priority.ROUTINE, 15, "units", 5, null);
        seedItem(bharati, "Cold Weather Gear", Priority.ROUTINE, 25, "units", 8, null);

        log.info("Seeded {} stations (MAITRI linked/synced, BHARATI unlinked/pending)", stationRepository.count());
    }

    private void seedItem(Station station, String name, Priority priority, int quantity, String unit,
                           int minThreshold, LocalDate expiryDate) {
        InventoryItem item = InventoryItem.builder()
                .name(name)
                .priority(priority)
                .quantity(quantity)
                .unit(unit)
                .minThreshold(minThreshold)
                .expiryDate(expiryDate)
                .build();
        inventoryService.create(item, station.getId());
    }
}
