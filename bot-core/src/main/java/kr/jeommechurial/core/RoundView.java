package kr.jeommechurial.core;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoundView(UUID id, String title, String roomName, RoundStatus status, String winner,
                        List<MenuEntry> entries, List<String> candidates, Instant createdAt, Instant updatedAt,
                        Instant drawAt) {}
