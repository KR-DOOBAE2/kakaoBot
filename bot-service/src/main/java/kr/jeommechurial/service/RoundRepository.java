package kr.jeommechurial.service;

import kr.jeommechurial.core.*;
import java.util.*;

public interface RoundRepository {
    LunchRound create(UUID ownerId, String title, String roomName, String joinTokenHash);
    Optional<LunchRound> find(UUID id);
    List<MenuEntry> entries(UUID roundId);
    MenuEntry upsertEntry(UUID roundId, UUID participantId, String participantName, String menu);
    void deleteEntry(UUID roundId, UUID participantId);
    List<String> candidates(UUID roundId);
    boolean draw(UUID roundId, String winner, long expectedVersion);
    boolean close(UUID roundId, long expectedVersion);
}
