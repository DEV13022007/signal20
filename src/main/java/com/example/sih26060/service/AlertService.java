package com.example.sih26060.service;

import com.example.sih26060.dto.Alert;
import com.example.sih26060.entity.AlertCategory;
import com.example.sih26060.entity.AlertSeverity;
import com.example.sih26060.entity.Station;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Raises an alert whenever an inventory item crosses below its threshold, equipment
 * degrades/fails, or a crew member's health goes CRITICAL. Alerts are broadcast live over
 * STOMP and kept in a small in-memory buffer so a client that (re)connects after the event
 * still sees recent history via AlertController.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private static final int HISTORY_LIMIT = 200;

    private final SimpMessagingTemplate messagingTemplate;
    private final AtomicLong idSequence = new AtomicLong();
    private final Deque<Alert> history = new ArrayDeque<>();

    public synchronized void raise(AlertSeverity severity, AlertCategory category, Station station,
                                    String entityType, Long entityId, String message) {
        Alert alert = new Alert(
                idSequence.incrementAndGet(),
                severity,
                category,
                station != null ? station.getId() : null,
                station != null ? station.getName() : null,
                entityType,
                entityId,
                message,
                Instant.now());

        history.addFirst(alert);
        while (history.size() > HISTORY_LIMIT) {
            history.removeLast();
        }

        log.info("ALERT [{}/{}] {}", severity, category, message);
        messagingTemplate.convertAndSend("/topic/alerts", alert);
    }

    public synchronized List<Alert> getRecent() {
        return List.copyOf(history);
    }
}
