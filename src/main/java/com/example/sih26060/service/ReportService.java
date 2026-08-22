package com.example.sih26060.service;

import com.example.sih26060.dto.StationSyncStatus;
import com.example.sih26060.entity.Equipment;
import com.example.sih26060.entity.InventoryItem;
import com.example.sih26060.entity.Personnel;
import com.example.sih26060.entity.Priority;
import com.example.sih26060.entity.Station;
import com.example.sih26060.entity.SyncStatus;
import com.example.sih26060.repository.EquipmentRepository;
import com.example.sih26060.repository.InventoryItemRepository;
import com.example.sih26060.repository.PersonnelRepository;
import com.example.sih26060.repository.StationRepository;
import com.example.sih26060.repository.SyncRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Renders a station status report (inventory, crew, equipment, sync stats) to PDF using
 * PDFBox directly — no template engine, just a small hand-rolled layout cursor since the
 * report is short and single-column.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final float MARGIN = 50f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final int EXPIRY_WARNING_DAYS = 90;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'");

    private final StationRepository stationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PersonnelRepository personnelRepository;
    private final EquipmentRepository equipmentRepository;
    private final SyncRecordRepository syncRecordRepository;
    private final SyncQueueService syncQueueService;

    @Transactional(readOnly = true)
    public byte[] generateStationReport(Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new EntityNotFoundException("Station not found: " + stationId));
        List<InventoryItem> inventory = inventoryItemRepository.findByStation_Id(stationId);
        List<Personnel> crew = personnelRepository.findByStation_Id(stationId);
        List<Equipment> equipment = equipmentRepository.findByStation_Id(stationId);
        StationSyncStatus syncStatus = syncQueueService.getStationStatus(stationId);
        long totalSynced = syncRecordRepository.countByStation_IdAndStatus(stationId, SyncStatus.SYNCED);

        try (PDDocument document = new PDDocument()) {
            ReportCursor cursor = new ReportCursor(document);
            try {
                writeHeader(cursor, station);
                writeInventorySection(cursor, inventory);
                writeCrewSection(cursor, crew);
                writeEquipmentSection(cursor, equipment);
                writeSyncSection(cursor, syncStatus, totalSynced, station);
            } finally {
                cursor.close();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate report for station " + stationId, e);
        }
    }

    private void writeHeader(ReportCursor c, Station station) throws IOException {
        c.writeLine(c.bold(16), "PolarConnect — Station Status Report");
        c.writeLine(c.bold(13), "%s (%s)".formatted(station.getName(), station.getCode()));
        c.writeLine(c.regular(10), "%s · Capacity %s · %s season · Operational since %s".formatted(
                station.getCountry() != null ? station.getCountry() : "Unknown territory",
                station.getCapacity() != null ? station.getCapacity() : "—",
                station.getCurrentSeason() != null ? station.getCurrentSeason() : "—",
                station.getOperationalSinceYear() != null ? station.getOperationalSinceYear() : "—"));
        c.writeLine(c.regular(10), "Satellite link: %s".formatted(
                Boolean.TRUE.equals(station.getSatelliteLinkActive()) ? "ACTIVE" : "DOWN"));
        c.writeLine(c.italic(9), "Generated " + TIMESTAMP_FORMAT.format(java.time.Instant.now()
                .atZone(java.time.ZoneOffset.UTC)));
        c.gap(10);
    }

    private void writeInventorySection(ReportCursor c, List<InventoryItem> items) throws IOException {
        c.writeSectionTitle("Inventory Position · " + items.size() + " items");
        if (items.isEmpty()) {
            c.writeLine(c.italic(10), "No inventory recorded.");
            c.gap(8);
            return;
        }
        LocalDate today = LocalDate.now();
        List<InventoryItem> sorted = items.stream()
                .sorted(Comparator.comparing(i -> i.getPriority().ordinal()))
                .toList();
        for (InventoryItem item : sorted) {
            boolean low = item.getMinThreshold() != null && item.getQuantity() <= item.getMinThreshold();
            Long daysUntilExpiry = item.getExpiryDate() != null
                    ? today.until(item.getExpiryDate(), java.time.temporal.ChronoUnit.DAYS)
                    : null;
            boolean expired = daysUntilExpiry != null && daysUntilExpiry < 0;
            boolean expiring = daysUntilExpiry != null && !expired && daysUntilExpiry <= EXPIRY_WARNING_DAYS;

            StringBuilder flags = new StringBuilder();
            if (expired) flags.append(" [EXPIRED]");
            else if (expiring) flags.append(" [EXPIRING · %dd]".formatted(daysUntilExpiry));
            if (low) flags.append(" [LOW STOCK]");

            String line = "%-8s %-28s qty %-6s thr %-6s exp %-12s%s".formatted(
                    item.getPriority(), truncate(item.getName(), 28), item.getQuantity(),
                    item.getMinThreshold() != null ? item.getMinThreshold() : "—",
                    item.getExpiryDate() != null ? item.getExpiryDate() : "—",
                    flags);
            c.writeLine(c.mono(9), line);
        }
        c.gap(8);
    }

    private void writeCrewSection(ReportCursor c, List<Personnel> crew) throws IOException {
        c.writeSectionTitle("Crew Roster · " + crew.size() + " personnel");
        if (crew.isEmpty()) {
            c.writeLine(c.italic(10), "No crew recorded.");
            c.gap(8);
            return;
        }
        List<Personnel> sorted = crew.stream()
                .sorted(Comparator.comparing((Personnel p) -> p.getHealthStatus().ordinal()).reversed())
                .toList();
        for (Personnel person : sorted) {
            String line = "%-24s %-22s %s → %-12s [%s]".formatted(
                    truncate(person.getName(), 24), truncate(person.getRole(), 22),
                    person.getRotationStart() != null ? person.getRotationStart() : "—",
                    person.getRotationEnd() != null ? person.getRotationEnd() : "—",
                    person.getHealthStatus());
            c.writeLine(c.mono(9), line);
        }
        c.gap(8);
    }

    private void writeEquipmentSection(ReportCursor c, List<Equipment> equipment) throws IOException {
        c.writeSectionTitle("Equipment Status · " + equipment.size() + " units");
        if (equipment.isEmpty()) {
            c.writeLine(c.italic(10), "No equipment recorded.");
            c.gap(8);
            return;
        }
        LocalDate today = LocalDate.now();
        List<Equipment> sorted = equipment.stream()
                .sorted(Comparator.comparing((Equipment e) -> e.getStatus().ordinal()).reversed())
                .toList();
        for (Equipment item : sorted) {
            boolean overdue = item.getNextServiceDue() != null && item.getNextServiceDue().isBefore(today);
            String line = "%-10s %-26s [%-11s] service %-12s due %-12s%s".formatted(
                    item.getType(), truncate(item.getName(), 26), item.getStatus(),
                    item.getLastServiceDate() != null ? item.getLastServiceDate() : "—",
                    item.getNextServiceDue() != null ? item.getNextServiceDue() : "—",
                    overdue ? " [OVERDUE]" : "");
            c.writeLine(c.mono(9), line);
        }
        c.gap(8);
    }

    private void writeSyncSection(ReportCursor c, StationSyncStatus syncStatus, long totalSynced, Station station)
            throws IOException {
        c.writeSectionTitle("Sync Statistics");
        c.writeLine(c.regular(10), "Pending operations: " + syncStatus.pendingCount());
        c.writeLine(c.regular(10), "Synced operations: " + totalSynced);
        for (Priority priority : Priority.values()) {
            long count = syncStatus.pendingByPriority().getOrDefault(priority, 0L);
            if (count > 0) {
                c.writeLine(c.mono(9), "  %-10s %d pending".formatted(priority, count));
            }
        }
        c.gap(8);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
    }

    /**
     * Tracks the current page/content-stream and vertical cursor position, opening a new
     * page automatically when the current one runs out of room.
     */
    private static final class ReportCursor {
        private final PDDocument document;
        private final PDFont regularFont;
        private final PDFont boldFont;
        private final PDFont italicFont;
        private final PDFont monoFont;
        private PDPageContentStream stream;
        private float y;

        ReportCursor(PDDocument document) throws IOException {
            this.document = document;
            this.regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            this.boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            this.italicFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
            this.monoFont = new PDType1Font(Standard14Fonts.FontName.COURIER);
            newPage();
        }

        record Style(PDFont font, float size) {
        }

        Style bold(float size) {
            return new Style(boldFont, size);
        }

        Style regular(float size) {
            return new Style(regularFont, size);
        }

        Style italic(float size) {
            return new Style(italicFont, size);
        }

        Style mono(float size) {
            return new Style(monoFont, size);
        }

        void writeSectionTitle(String title) throws IOException {
            gap(6);
            drawRule();
            writeLine(bold(12), title);
        }

        void writeLine(Style style, String text) throws IOException {
            float lineHeight = style.size() + 4;
            ensureSpace(lineHeight);
            y -= lineHeight;
            stream.beginText();
            stream.setFont(style.font(), style.size());
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(sanitize(text));
            stream.endText();
        }

        void gap(float height) throws IOException {
            ensureSpace(height);
            y -= height;
        }

        void drawRule() throws IOException {
            ensureSpace(4);
            stream.setLineWidth(0.5f);
            stream.moveTo(MARGIN, y);
            stream.lineTo(PAGE_WIDTH - MARGIN, y);
            stream.stroke();
            y -= 6;
        }

        void ensureSpace(float needed) throws IOException {
            if (y - needed < MARGIN) {
                stream.close();
                newPage();
            }
        }

        private void newPage() throws IOException {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PAGE_HEIGHT - MARGIN;
        }

        void close() throws IOException {
            stream.close();
        }

        private static String sanitize(String text) {
            // Standard 14 fonts only support WinAnsi/Latin-1; strip anything outside it
            // (em dashes etc. are replaced with a plain hyphen) rather than let PDFBox throw.
            StringBuilder sb = new StringBuilder(text.length());
            for (char ch : text.toCharArray()) {
                sb.append(ch < 256 ? ch : '-');
            }
            return sb.toString();
        }
    }
}
