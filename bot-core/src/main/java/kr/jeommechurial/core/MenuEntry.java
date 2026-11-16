package kr.jeommechurial.core;

import java.time.Instant;
import java.util.UUID;

public record MenuEntry(UUID id, UUID roundId, UUID participantId, String participantName,
                        String menu, Instant createdAt, Instant updatedAt) {}
