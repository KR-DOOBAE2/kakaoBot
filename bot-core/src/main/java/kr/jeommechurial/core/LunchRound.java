package kr.jeommechurial.core;

import java.time.Instant;
import java.util.UUID;

public record LunchRound(UUID id, UUID ownerId, String title, String roomName, String joinTokenHash,
                         RoundStatus status, String winner, long version, Instant createdAt, Instant updatedAt,
                         Instant drawAt) {}
