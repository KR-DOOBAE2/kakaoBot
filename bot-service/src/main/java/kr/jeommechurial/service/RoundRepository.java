package kr.jeommechurial.service;

import kr.jeommechurial.core.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public interface RoundRepository {
    LunchRound create(UUID ownerId, String title, String roomName, String joinTokenHash, Instant drawAt,
                      String officeAddress, LocalTime lunchStartTime, LocalTime lunchEndTime);
    Optional<LunchRound> find(UUID id);
    List<LunchRound> findDueOpen(Instant now);
    List<LunchRound> findBefore(Instant midnight);
    List<MenuEntry> entries(UUID roundId);
    MenuEntry addEntry(UUID roundId, UUID participantId, String participantName, String menu);
    void deleteEntry(UUID roundId, UUID participantId);
    void deleteEntry(UUID roundId, UUID participantId, UUID entryId);
    List<String> candidates(UUID roundId);
    boolean draw(UUID roundId, String winner, long expectedVersion, boolean reroll, Instant drawnAt);
    boolean close(UUID roundId, long expectedVersion);
    boolean updateSettings(UUID roundId, String officeAddress, LocalTime lunchStartTime, LocalTime lunchEndTime,
                           Instant nextDrawAt, long expectedVersion);
    boolean resetForNewDay(UUID roundId, Instant nextDrawAt, long expectedVersion);
    void saveSatisfaction(UUID roundId, LocalDate day, UUID participantId, Satisfaction satisfaction);
    Optional<Satisfaction> satisfaction(UUID roundId, LocalDate day, UUID participantId);
    Map<Satisfaction, Long> satisfactionCounts(UUID roundId, LocalDate day);
}
