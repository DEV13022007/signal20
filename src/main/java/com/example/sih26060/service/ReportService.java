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

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Renders a station status report (inventory, crew, equipment, sync stats) to PDF using
 * PDFBox directly — no template engine, just a small hand-rolled layout cursor since the
 * report is short and single-column. Tabular data is drawn as real columns (measured text
 * positioned at fixed x-offsets) rather than monospace-padded strings, so rows can't overflow
 * the page width the way fixed-width string formatting can.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final float MARGIN = 50f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final int EXPIRY_WARNING_DAYS = 90;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'");

    private static final Color ACCENT = new Color(37, 99, 235);
    private static final Color GRAY_700 = new Color(55, 65, 81);
    private static final Color GRAY_500 = new Color(107, 114, 128);
    private static final Color HEADER_BG = new Color(241, 245, 249);
    private static final Color ROW_ALT_BG = new Color(249, 250, 251);
    private static final Color BORDER = new Color(209, 213, 219);
    private static final Color RED = new Color(185, 28, 28);
    private static final Color RED_BG = new Color(254, 226, 226);
    private static final Color AMBER = new Color(180, 83, 9);
    private static final Color AMBER_BG = new Color(254, 243, 199);
    private static final Color GREEN = new Color(21, 128, 61);
    private static final Color GREEN_BG = new Color(220, 252, 231);

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
                writeSyncSection(cursor, syncStatus, totalSynced);
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
        c.titleLine("PolarConnect", "Station Status Report");
        c.writeLine(c.bold(13), "%s (%s)".formatted(station.getName(), station.getCode()));
        c.writeLine(c.regular(9.5f, GRAY_700), "%s  ·  Capacity %s  ·  %s season  ·  Operational since %s".formatted(
                station.getCountry() != null ? station.getCountry() : "Unknown territory",
                station.getCapacity() != null ? station.getCapacity() : "—",
                station.getCurrentSeason() != null ? station.getCurrentSeason() : "—",
                station.getOperationalSinceYear() != null ? station.getOperationalSinceYear() : "—"));
        boolean linkActive = Boolean.TRUE.equals(station.getSatelliteLinkActive());
        c.writeLineWithBadge("Satellite link:", linkActive ? "ACTIVE" : "DOWN",
                linkActive ? GREEN_BG : RED_BG, linkActive ? GREEN : RED);
        c.writeLine(c.italic(8.5f, GRAY_500), "Generated " + TIMESTAMP_FORMAT.format(java.time.Instant.now()
                .atZone(java.time.ZoneOffset.UTC)));
        c.gap(6);
    }

    private void writeInventorySection(ReportCursor c, List<InventoryItem> items) throws IOException {
        c.writeSectionTitle("Inventory Position · " + items.size() + " items");
        if (items.isEmpty()) {
            c.writeLine(c.italic(10, GRAY_500), "No inventory recorded.");
            c.gap(8);
            return;
        }
        ReportCursor.Column[] columns = {
                new ReportCursor.Column("Category", 55),
                new ReportCursor.Column("Item", 150),
                new ReportCursor.Column("Qty", 40, true),
                new ReportCursor.Column("Min", 55, true),
                new ReportCursor.Column("Expiry", 75),
                new ReportCursor.Column("Status", 120),
        };
        c.tableHeader(columns);

        LocalDate today = LocalDate.now();
        List<InventoryItem> sorted = items.stream()
                .sorted(Comparator.comparing(i -> i.getPriority().ordinal()))
                .toList();
        boolean zebra = false;
        for (InventoryItem item : sorted) {
            boolean low = item.getMinThreshold() != null && item.getQuantity() <= item.getMinThreshold();
            Long daysUntilExpiry = item.getExpiryDate() != null
                    ? today.until(item.getExpiryDate(), java.time.temporal.ChronoUnit.DAYS)
                    : null;
            boolean expired = daysUntilExpiry != null && daysUntilExpiry < 0;
            boolean expiring = daysUntilExpiry != null && !expired && daysUntilExpiry <= EXPIRY_WARNING_DAYS;

            List<String> flags = new ArrayList<>();
            if (expired) flags.add("EXPIRED");
            else if (expiring) flags.add("EXPIRING · %dd".formatted(daysUntilExpiry));
            if (low) flags.add("LOW STOCK");
            String status = flags.isEmpty() ? "OK" : String.join(" · ", flags);
            Color statusColor = expired ? RED : (expiring || low ? AMBER : GREEN);

            c.tableRow(columns, new String[]{
                    item.getPriority().toString(),
                    item.getName(),
                    String.valueOf(item.getQuantity()),
                    item.getMinThreshold() != null ? String.valueOf(item.getMinThreshold()) : "—",
                    item.getExpiryDate() != null ? item.getExpiryDate().toString() : "—",
                    status,
            }, new Color[]{null, null, null, null, null, statusColor}, zebra);
            zebra = !zebra;
        }
        c.tableFooterRule(columns);
        c.gap(10);
    }

    private void writeCrewSection(ReportCursor c, List<Personnel> crew) throws IOException {
        c.writeSectionTitle("Crew Roster · " + crew.size() + " personnel");
        if (crew.isEmpty()) {
            c.writeLine(c.italic(10, GRAY_500), "No crew recorded.");
            c.gap(8);
            return;
        }
        ReportCursor.Column[] columns = {
                new ReportCursor.Column("Name", 130),
                new ReportCursor.Column("Role", 115),
                new ReportCursor.Column("Rotation", 140),
                new ReportCursor.Column("Health", 110),
        };
        c.tableHeader(columns);

        List<Personnel> sorted = crew.stream()
                .sorted(Comparator.comparing((Personnel p) -> p.getHealthStatus().ordinal()).reversed())
                .toList();
        boolean zebra = false;
        for (Personnel person : sorted) {
            Color healthColor = switch (person.getHealthStatus()) {
                case CRITICAL -> RED;
                case MONITORING -> AMBER;
                case NOMINAL -> GREEN;
            };
            String rotation = "%s - %s".formatted(
                    person.getRotationStart() != null ? person.getRotationStart() : "—",
                    person.getRotationEnd() != null ? person.getRotationEnd() : "—");
            c.tableRow(columns, new String[]{
                    person.getName(),
                    person.getRole(),
                    rotation,
                    person.getHealthStatus().toString(),
            }, new Color[]{null, null, null, healthColor}, zebra);
            zebra = !zebra;
        }
        c.tableFooterRule(columns);
        c.gap(10);
    }

    private void writeEquipmentSection(ReportCursor c, List<Equipment> equipment) throws IOException {
        c.writeSectionTitle("Equipment Status · " + equipment.size() + " units");
        if (equipment.isEmpty()) {
            c.writeLine(c.italic(10, GRAY_500), "No equipment recorded.");
            c.gap(8);
            return;
        }
        ReportCursor.Column[] columns = {
                new ReportCursor.Column("Type", 65),
                new ReportCursor.Column("Name", 145),
                new ReportCursor.Column("Status", 90),
                new ReportCursor.Column("Last Service", 75),
                new ReportCursor.Column("Next Due", 120),
        };
        c.tableHeader(columns);

        LocalDate today = LocalDate.now();
        List<Equipment> sorted = equipment.stream()
                .sorted(Comparator.comparing((Equipment e) -> e.getStatus().ordinal()).reversed())
                .toList();
        boolean zebra = false;
        for (Equipment item : sorted) {
            boolean overdue = item.getNextServiceDue() != null && item.getNextServiceDue().isBefore(today);
            Color statusColor = switch (item.getStatus()) {
                case FAILED -> RED;
                case DEGRADED -> AMBER;
                case OPERATIONAL -> GREEN;
            };
            String nextDue = (item.getNextServiceDue() != null ? item.getNextServiceDue().toString() : "—")
                    + (overdue ? "  (OVERDUE)" : "");
            c.tableRow(columns, new String[]{
                    item.getType().toString(),
                    item.getName(),
                    item.getStatus().toString(),
                    item.getLastServiceDate() != null ? item.getLastServiceDate().toString() : "—",
                    nextDue,
            }, new Color[]{null, null, statusColor, null, overdue ? RED : null}, zebra);
            zebra = !zebra;
        }
        c.tableFooterRule(columns);
        c.gap(10);
    }

    private void writeSyncSection(ReportCursor c, StationSyncStatus syncStatus, long totalSynced)
            throws IOException {
        c.writeSectionTitle("Sync Statistics");
        c.writeStat("Pending operations:  ", String.valueOf(syncStatus.pendingCount()),
                syncStatus.pendingCount() > 0 ? AMBER : GREEN);
        c.writeStat("Synced operations:  ", String.valueOf(totalSynced), GRAY_700);
        for (Priority priority : Priority.values()) {
            long count = syncStatus.pendingByPriority().getOrDefault(priority, 0L);
            if (count > 0) {
                c.writeLine(c.regular(9, GRAY_500), "-  %s: %d pending".formatted(priority, count));
            }
        }
        c.gap(8);
    }

    /**
     * Tracks the current page/content-stream and vertical cursor position, opening a new
     * page automatically when the current one runs out of room. Also hosts the column-table
     * drawing helpers used by the report sections above.
     */
    private static final class ReportCursor {
        private static final float CELL_PAD = 4f;

        private final PDDocument document;
        private final PDFont regularFont;
        private final PDFont boldFont;
        private final PDFont italicFont;
        private PDPageContentStream stream;
        private float y;

        ReportCursor(PDDocument document) throws IOException {
            this.document = document;
            this.regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            this.boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            this.italicFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
            newPage();
        }

        record Column(String header, float width, boolean rightAlign) {
            Column(String header, float width) {
                this(header, width, false);
            }
        }

        record Style(PDFont font, float size, Color color) {
        }

        Style bold(float size) {
            return new Style(boldFont, size, Color.BLACK);
        }

        Style regular(float size, Color color) {
            return new Style(regularFont, size, color);
        }

        Style italic(float size, Color color) {
            return new Style(italicFont, size, color);
        }

        void titleLine(String brand, String subtitle) throws IOException {
            float size = 18f;
            float lineHeight = size + 8;
            ensureSpace(lineHeight);
            y -= lineHeight;
            drawText(boldFont, size, ACCENT, brand, MARGIN, y);
            float brandWidth = textWidth(boldFont, size, brand);
            drawText(regularFont, 12f, GRAY_700, subtitle, MARGIN + brandWidth + 8, y + 3);
        }

        void writeSectionTitle(String title) throws IOException {
            gap(8);
            drawRule();
            writeLine(bold(12), title);
        }

        void writeLine(Style style, String text) throws IOException {
            float lineHeight = style.size() + 5;
            ensureSpace(lineHeight);
            y -= lineHeight;
            drawText(style.font(), style.size(), style.color(), text, MARGIN, y);
        }

        void writeLineWithBadge(String label, String badgeText, Color badgeBg, Color badgeFg) throws IOException {
            float size = 9.5f;
            float lineHeight = size + 6;
            ensureSpace(lineHeight);
            y -= lineHeight;
            drawText(regularFont, size, GRAY_700, label, MARGIN, y);
            float labelWidth = textWidth(regularFont, size, label);
            drawBadge(badgeText, MARGIN + labelWidth + 6, y, badgeBg, badgeFg);
        }

        void writeStat(String label, String value, Color valueColor) throws IOException {
            float size = 10f;
            float lineHeight = size + 6;
            ensureSpace(lineHeight);
            y -= lineHeight;
            drawText(regularFont, size, GRAY_700, label, MARGIN, y);
            float labelWidth = textWidth(regularFont, size, label);
            drawText(boldFont, size, valueColor, value, MARGIN + labelWidth, y);
        }

        void tableHeader(Column[] columns) throws IOException {
            float rowHeight = 18f;
            ensureSpace(rowHeight);
            y -= rowHeight;
            float width = totalWidth(columns);
            fillRect(MARGIN, y, width, rowHeight, HEADER_BG);
            float x = MARGIN;
            for (Column column : columns) {
                drawCell(boldFont, 8.5f, GRAY_700, column.header(), x, y + 6.5f, column.width(), column.rightAlign());
                x += column.width();
            }
            strokeLine(MARGIN, y, MARGIN + width, y, BORDER);
        }

        void tableRow(Column[] columns, String[] values, Color[] valueColors, boolean zebra) throws IOException {
            float rowHeight = 15f;
            ensureSpace(rowHeight);
            y -= rowHeight;
            float width = totalWidth(columns);
            if (zebra) {
                fillRect(MARGIN, y, width, rowHeight, ROW_ALT_BG);
            }
            float x = MARGIN;
            for (int i = 0; i < columns.length; i++) {
                Color color = valueColors != null && valueColors[i] != null ? valueColors[i] : Color.BLACK;
                drawCell(regularFont, 8.5f, color, values[i], x, y + 4.5f, columns[i].width(), columns[i].rightAlign());
                x += columns[i].width();
            }
        }

        void tableFooterRule(Column[] columns) throws IOException {
            strokeLine(MARGIN, y, MARGIN + totalWidth(columns), y, BORDER);
        }

        void gap(float height) throws IOException {
            ensureSpace(height);
            y -= height;
        }

        void drawRule() throws IOException {
            ensureSpace(4);
            strokeLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, BORDER);
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

        private static float totalWidth(Column[] columns) {
            float total = 0;
            for (Column column : columns) {
                total += column.width();
            }
            return total;
        }

        private void drawCell(PDFont font, float size, Color color, String text, float x, float baselineY,
                float width, boolean rightAlign) throws IOException {
            String fitted = fitToWidth(font, size, sanitize(text), width - CELL_PAD * 2);
            float tx;
            if (rightAlign) {
                tx = x + width - CELL_PAD - textWidth(font, size, fitted);
            } else {
                tx = x + CELL_PAD;
            }
            drawText(font, size, color, fitted, tx, baselineY);
        }

        private void drawText(PDFont font, float size, Color color, String text, float x, float baselineY)
                throws IOException {
            stream.setNonStrokingColor(color);
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(x, baselineY);
            stream.showText(sanitize(text));
            stream.endText();
        }

        private void drawBadge(String text, float x, float baselineY, Color bg, Color fg) throws IOException {
            float size = 8f;
            float textW = textWidth(boldFont, size, text);
            fillRect(x, baselineY - 2.5f, textW + 10f, size + 5f, bg);
            drawText(boldFont, size, fg, text, x + 5f, baselineY);
        }

        private void fillRect(float x, float y, float width, float height, Color color) throws IOException {
            stream.setNonStrokingColor(color);
            stream.addRect(x, y, width, height);
            stream.fill();
        }

        private void strokeLine(float x1, float y1, float x2, float y2, Color color) throws IOException {
            stream.setStrokingColor(color);
            stream.setLineWidth(0.75f);
            stream.moveTo(x1, y1);
            stream.lineTo(x2, y2);
            stream.stroke();
        }

        private static float textWidth(PDFont font, float size, String text) {
            try {
                return font.getStringWidth(sanitize(text)) / 1000f * size;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /** Truncates text with a trailing "..." so it never overflows the given column width. */
        private static String fitToWidth(PDFont font, float size, String text, float maxWidth) throws IOException {
            if (textWidth(font, size, text) <= maxWidth) {
                return text;
            }
            String ellipsis = "...";
            float ellipsisWidth = textWidth(font, size, ellipsis);
            StringBuilder sb = new StringBuilder();
            float width = 0;
            for (char ch : text.toCharArray()) {
                float charWidth = font.getStringWidth(String.valueOf(ch)) / 1000f * size;
                if (width + charWidth + ellipsisWidth > maxWidth) {
                    break;
                }
                sb.append(ch);
                width += charWidth;
            }
            return sb + ellipsis;
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