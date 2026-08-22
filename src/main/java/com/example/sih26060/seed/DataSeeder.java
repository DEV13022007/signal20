package com.example.sih26060.seed;

import com.example.sih26060.entity.HealthStatus;
import com.example.sih26060.entity.InventoryItem;
import com.example.sih26060.entity.Personnel;
import com.example.sih26060.entity.Priority;
import com.example.sih26060.entity.Season;
import com.example.sih26060.entity.Station;
import com.example.sih26060.repository.InventoryItemRepository;
import com.example.sih26060.repository.PersonnelRepository;
import com.example.sih26060.repository.StationRepository;
import com.example.sih26060.repository.SyncRecordRepository;
import com.example.sih26060.service.InventoryService;
import com.example.sih26060.service.PersonnelService;
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
    private final PersonnelRepository personnelRepository;
    private final SyncRecordRepository syncRecordRepository;
    private final InventoryService inventoryService;
    private final PersonnelService personnelService;

    @Value("${polarconnect.seed.enabled:true}")
    private boolean enabled;

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        syncRecordRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        personnelRepository.deleteAll();
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

        // Maitri: 8 crew, one under MONITORING.
        seedCrew(maitri, "Ananya Rao", "Station Leader", today.minusDays(120), today.plusDays(160), HealthStatus.NOMINAL);
        seedCrew(maitri, "Vikram Sethi", "Medical Officer", today.minusDays(120), today.plusDays(160), HealthStatus.NOMINAL);
        seedCrew(maitri, "Rahul Menon", "Chief Engineer", today.minusDays(95), today.plusDays(185), HealthStatus.MONITORING);
        seedCrew(maitri, "Priya Nair", "Communications Officer", today.minusDays(95), today.plusDays(185), HealthStatus.NOMINAL);
        seedCrew(maitri, "Suresh Pillai", "Cook", today.minusDays(60), today.plusDays(220), HealthStatus.NOMINAL);
        seedCrew(maitri, "Devika Kulkarni", "Glaciologist", today.minusDays(60), today.plusDays(220), HealthStatus.NOMINAL);
        seedCrew(maitri, "Arjun Bhatt", "Mechanic", today.minusDays(30), today.plusDays(250), HealthStatus.NOMINAL);
        seedCrew(maitri, "Meera Iyer", "Meteorologist", today.minusDays(30), today.plusDays(250), HealthStatus.NOMINAL);

        // Bharati: 12 crew, one under CRITICAL to give the alerting module (task 4) and
        // the report module (task 5) a live case to demonstrate against.
        seedCrew(bharati, "Kabir Malhotra", "Station Leader", today.minusDays(140), today.plusDays(140), HealthStatus.NOMINAL);
        seedCrew(bharati, "Neha Kapoor", "Medical Officer", today.minusDays(140), today.plusDays(140), HealthStatus.NOMINAL);
        seedCrew(bharati, "Rajesh Kumar", "Chief Engineer", today.minusDays(110), today.plusDays(170), HealthStatus.CRITICAL);
        seedCrew(bharati, "Sunita Deshmukh", "Communications Officer", today.minusDays(110), today.plusDays(170), HealthStatus.NOMINAL);
        seedCrew(bharati, "Amit Verma", "Cook", today.minusDays(80), today.plusDays(200), HealthStatus.NOMINAL);
        seedCrew(bharati, "Lakshmi Narayan", "Marine Biologist", today.minusDays(80), today.plusDays(200), HealthStatus.NOMINAL);
        seedCrew(bharati, "Farhan Sheikh", "Mechanic", today.minusDays(50), today.plusDays(230), HealthStatus.NOMINAL);
        seedCrew(bharati, "Divya Krishnan", "Meteorologist", today.minusDays(50), today.plusDays(230), HealthStatus.NOMINAL);
        seedCrew(bharati, "Rohan Chawla", "Electrician", today.minusDays(20), today.plusDays(260), HealthStatus.NOMINAL);
        seedCrew(bharati, "Pooja Reddy", "Logistics Officer", today.minusDays(20), today.plusDays(260), HealthStatus.NOMINAL);
        seedCrew(bharati, "Sameer Joshi", "Glaciologist", today.minusDays(10), today.plusDays(270), HealthStatus.MONITORING);
        seedCrew(bharati, "Ishaan Ahluwalia", "Vehicle Operator", today.minusDays(10), today.plusDays(270), HealthStatus.NOMINAL);

        log.info("Seeded {} stations, {} inventory items, {} crew", stationRepository.count(),
                inventoryItemRepository.count(), personnelRepository.count());
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

    private void seedCrew(Station station, String name, String role, LocalDate rotationStart,
                           LocalDate rotationEnd, HealthStatus healthStatus) {
        Personnel person = Personnel.builder()
                .name(name)
                .role(role)
                .rotationStart(rotationStart)
                .rotationEnd(rotationEnd)
                .healthStatus(healthStatus)
                .build();
        personnelService.create(person, station.getId());
    }
}
