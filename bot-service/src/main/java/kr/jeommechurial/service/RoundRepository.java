package kr.jeommechurial.service;

import kr.jeommechurial.core.*;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;

public interface RoundRepository {
    LunchRound create(UUID ownerId, String title, String roomName, String joinTokenHash, Instant drawAt,
                      String officeAddress, LocalTime lunchStartTime, LocalTime lunchEndTime);
    Optional<LunchRound> find(UUID id);
    List<LunchRound> findDueOpen(Instant now);
    List<MenuEntry> entries(UUID roundId);
    MenuEntry upsertEntry(UUID roundId, UUID participantId, String participantName, String menu);
    void deleteEntry(UUID roundId, UUID participantId);
    List<String> candidates(UUID roundId);
    boolean draw(UUID roundId, String winner, long expectedVersion, boolean reroll);
    boolean close(UUID roundId, long expectedVersion);
}
