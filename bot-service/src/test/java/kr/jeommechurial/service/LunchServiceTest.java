package kr.jeommechurial.service;

import kr.jeommechurial.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LunchServiceTest {
    private FakeRepository repo;
    private LunchService service;
    private Clock clock;

    @BeforeEach void setup() {
        repo = new FakeRepository();
        clock = Clock.fixed(Instant.parse("2026-11-16T01:00:00Z"), ZoneOffset.UTC);
        service = new LunchService(repo, "test-secret", clock);
    }

    @Test void duplicateMenusCollapseAndNamesAreAnonymous() {
        CreatedRound r = service.create("점심", "우리방");
        service.register(r.id(), r.joinToken(), UUID.randomUUID(), "실명 A", "김치찌개");
        service.register(r.id(), r.joinToken(), UUID.randomUUID(), "실명 B", "김치찌개");
        service.register(r.id(), r.joinToken(), UUID.randomUUID(), "실명 C", "돈까스");
        RoundView view = service.get(r.id(), r.joinToken());
        assertEquals(2, view.candidates().size());
        assertTrue(view.entries().stream().allMatch(e -> e.participantName().equals("익명")));
        assertEquals(Instant.parse("2026-11-16T01:45:00Z"), view.drawAt());
    }

    @Test void participantCanChangeAndCancelOnlyTheirOwnEntry() {
        CreatedRound r = service.create("점심", "우리방");
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        service.register(r.id(), r.joinToken(), a, "A", "국밥");
        service.register(r.id(), r.joinToken(), a, "A", "냉면");
        service.register(r.id(), r.joinToken(), b, "B", "국밥");
        service.cancel(r.id(), r.joinToken(), a);
        RoundView view = service.get(r.id(), r.joinToken());
        assertEquals(1, view.entries().size());
        assertEquals("국밥", view.entries().getFirst().menu());
    }

    @Test void drawAtCutoffIsStableAndRejectsLateEntries() {
        CreatedRound r = service.create("점심", "우리방");
        service.register(r.id(), r.joinToken(), UUID.randomUUID(), "A", "국밥");
        assertThrows(IllegalStateException.class, () -> service.draw(r.id(), r.ownerToken(), false));
        service = new LunchService(repo, "test-secret", Clock.fixed(Instant.parse("2026-11-16T01:45:00Z"), ZoneOffset.UTC));
        assertThrows(IllegalStateException.class, () -> service.register(r.id(), r.joinToken(), UUID.randomUUID(), "B", "냉면"));
        RoundView first = service.get(r.id(), r.joinToken());
        RoundView second = service.get(r.id(), r.joinToken());
        assertEquals("국밥", first.winner());
        assertEquals(first.winner(), second.winner());
        assertEquals(1, repo.drawCount);
        assertThrows(IllegalStateException.class, () -> service.draw(r.id(), r.ownerToken(), true));
    }

    @Test void emptyRoundClosesAtCutoff() {
        CreatedRound r = service.create("점심", "우리방");
        service = new LunchService(repo, "test-secret", Clock.fixed(Instant.parse("2026-11-16T01:45:00Z"), ZoneOffset.UTC));
        service.drawDueOpenRounds();
        assertEquals(RoundStatus.CLOSED, service.get(r.id(), r.joinToken()).status());
    }

    @Test void rejectsBadToken() {
        CreatedRound r = service.create("점심", "우리방");
        assertThrows(SecurityException.class, () -> service.get(r.id(), "wrong"));
    }

    static class FakeRepository implements RoundRepository {
        final Map<UUID, LunchRound> rounds = new HashMap<>();
        final Map<UUID, List<MenuEntry>> entries = new HashMap<>();
        int drawCount;
        public LunchRound create(UUID owner, String title, String room, String hash, Instant drawAt) {
            UUID id = UUID.randomUUID();
            LunchRound round = new LunchRound(id, owner, title, room, hash, RoundStatus.OPEN, null, 0, Instant.now(), Instant.now(), drawAt);
            rounds.put(id, round); entries.put(id, new ArrayList<>()); return round;
        }
        public Optional<LunchRound> find(UUID id) { return Optional.ofNullable(rounds.get(id)); }
        public List<LunchRound> findDueOpen(Instant now) { return rounds.values().stream().filter(r -> r.status() == RoundStatus.OPEN && r.drawAt() != null && !r.drawAt().isAfter(now)).toList(); }
        public List<MenuEntry> entries(UUID id) { return List.copyOf(entries.get(id)); }
        public MenuEntry upsertEntry(UUID rid, UUID pid, String name, String menu) {
            List<MenuEntry> list = entries.get(rid); list.removeIf(e -> e.participantId().equals(pid));
            MenuEntry entry = new MenuEntry(UUID.randomUUID(), rid, pid, name, menu, Instant.now(), Instant.now());
            list.add(entry); return entry;
        }
        public void deleteEntry(UUID rid, UUID pid) { entries.get(rid).removeIf(e -> e.participantId().equals(pid)); }
        public List<String> candidates(UUID id) { return entries.get(id).stream().map(e -> e.menu().strip()).distinct().toList(); }
        public boolean draw(UUID id, String winner, long version, boolean reroll) {
            LunchRound r = rounds.get(id);
            if (r.version() != version || r.status() != (reroll ? RoundStatus.DRAWN : RoundStatus.OPEN)) return false;
            rounds.put(id, new LunchRound(id, r.ownerId(), r.title(), r.roomName(), r.joinTokenHash(), RoundStatus.DRAWN, winner, version + 1, r.createdAt(), Instant.now(), r.drawAt()));
            drawCount++; return true;
        }
        public boolean close(UUID id, long version) {
            LunchRound r = rounds.get(id);
            if (r.version() != version || r.status() == RoundStatus.CLOSED) return false;
            rounds.put(id, new LunchRound(id, r.ownerId(), r.title(), r.roomName(), r.joinTokenHash(), RoundStatus.CLOSED, r.winner(), version + 1, r.createdAt(), Instant.now(), r.drawAt()));
            return true;
        }
    }
}
