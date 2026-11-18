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

    @Test void participantCanAddSeveralMenusAndCancelThemIndividually() {
        CreatedRound r = service.create("점심", "우리방");
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        service.register(r.id(), r.joinToken(), a, "A", "국밥");
        service.register(r.id(), r.joinToken(), a, "A", "냉면");
        service.register(r.id(), r.joinToken(), a, "A", "국밥");
        service.register(r.id(), r.joinToken(), b, "B", "국밥");
        RoundView before = service.get(r.id(), r.joinToken());
        assertEquals(3, before.entries().size());
        assertEquals(2, before.candidates().size());
        UUID aEntry = before.entries().stream().filter(e -> e.participantId().equals(a) && e.menu().equals("국밥")).findFirst().orElseThrow().id();
        service.cancel(r.id(), r.joinToken(), b, aEntry);
        assertEquals(3, service.get(r.id(), r.joinToken()).entries().size());
        service.cancel(r.id(), r.joinToken(), a, aEntry);
        RoundView view = service.get(r.id(), r.joinToken());
        assertEquals(2, view.entries().size());
        assertEquals(2, view.candidates().size());
        service.cancel(r.id(), r.joinToken(), a);
        assertEquals("국밥", service.get(r.id(), r.joinToken()).entries().getFirst().menu());
    }

    @Test void drawAtCutoffIsStableAndRejectsLateEntries() {
        CreatedRound r = service.create("점심", "우리방");
        service.register(r.id(), r.joinToken(), UUID.randomUUID(), "A", "국밥");
        service = new LunchService(repo, "test-secret", Clock.fixed(Instant.parse("2026-11-16T01:45:00Z"), ZoneOffset.UTC));
        assertThrows(IllegalStateException.class, () -> service.register(r.id(), r.joinToken(), UUID.randomUUID(), "B", "냉면"));
        RoundView first = service.get(r.id(), r.joinToken());
        RoundView second = service.get(r.id(), r.joinToken());
        assertEquals("국밥", first.winner());
        assertEquals(first.winner(), second.winner());
        assertEquals(1, repo.drawCount);
        assertThrows(IllegalStateException.class, () -> service.draw(r.id(), r.ownerToken(), true));
    }

    @Test void ownerCanRevealEarlyAndEveryoneKeepsTheSameResult() {
        CreatedRound r = service.create("점심", "우리방");
        UUID participant = UUID.randomUUID();
        service.register(r.id(), r.joinToken(), participant, "익명", "김치찌개");
        service.register(r.id(), r.joinToken(), participant, "익명", "돈까스");
        assertThrows(SecurityException.class, () -> service.draw(r.id(), "wrong", false));

        RoundView early = service.draw(r.id(), r.ownerToken(), false);
        assertEquals(RoundStatus.DRAWN, early.status());
        assertEquals(clock.instant(), early.drawAt());
        assertTrue(early.candidates().contains(early.winner()));
        assertThrows(IllegalStateException.class, () -> service.register(r.id(), r.joinToken(), participant, "익명", "초밥"));
        assertEquals(early.winner(), service.draw(r.id(), r.ownerToken(), false).winner());

        service = new LunchService(repo, "test-secret", Clock.fixed(Instant.parse("2026-11-16T01:45:00Z"), ZoneOffset.UTC));
        assertEquals(early.winner(), service.get(r.id(), r.joinToken()).winner());
        assertEquals(1, repo.drawCount);
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
    @Test void officeAddressAndMealTimesAreSavedWithRound() {
        CreatedRound r = service.create("점심", "우리방", "서울 강남구 테헤란로 123", LocalTime.NOON, LocalTime.of(13, 0));
        RoundView view = service.get(r.id(), r.joinToken());
        assertEquals("서울 강남구 테헤란로 123", view.officeAddress());
        assertEquals(LocalTime.NOON, view.lunchStartTime());
        assertEquals(LocalTime.of(13, 0), view.lunchEndTime());
        assertThrows(IllegalArgumentException.class, () -> service.create("점심", "우리방", "서울", LocalTime.of(13, 0), LocalTime.NOON));
    }
    @Test void onlyCreatorCanChangePersistentTeamSettings() {
        CreatedRound r = service.create("점심", "우리팀", "서울시청", LocalTime.NOON, LocalTime.of(13, 0));
        assertThrows(SecurityException.class, () -> service.updateSettings(r.id(), "wrong", "부산시청", LocalTime.of(11, 0), LocalTime.NOON));
        service.updateSettings(r.id(), r.ownerToken(), "부산시청", LocalTime.of(11, 0), LocalTime.NOON);
        RoundView view = service.get(r.id(), r.joinToken());
        assertEquals("부산시청", view.officeAddress());
        assertEquals(LocalTime.of(11, 0), view.lunchStartTime());
    }
    @Test void sameTeamLinkStartsFreshBallotNextDayAndKeepsSettings() {
        CreatedRound r = service.create("점심", "우리팀", "서울시청", LocalTime.NOON, LocalTime.of(13, 0));
        service.register(r.id(), r.joinToken(), UUID.randomUUID(), null, "매장 · 한식");
        service = new LunchService(repo, "test-secret", Clock.fixed(Instant.parse("2026-11-16T01:45:00Z"), ZoneOffset.UTC));
        assertEquals("매장 · 한식", service.get(r.id(), r.joinToken()).winner());
        service = new LunchService(repo, "test-secret", Clock.fixed(Instant.parse("2026-11-16T23:00:00Z"), ZoneOffset.UTC));
        RoundView next = service.get(r.id(), r.joinToken());
        assertEquals(RoundStatus.OPEN, next.status());
        assertTrue(next.entries().isEmpty());
        assertNull(next.winner());
        assertEquals("서울시청", next.officeAddress());
        assertEquals(Instant.parse("2026-11-17T01:45:00Z"), next.drawAt());
    }

    static class FakeRepository implements RoundRepository {
        final Map<UUID, LunchRound> rounds = new HashMap<>();
        final Map<UUID, List<MenuEntry>> entries = new HashMap<>();
        int drawCount;
        public LunchRound create(UUID owner, String title, String room, String hash, Instant drawAt,
                                 String officeAddress, LocalTime lunchStartTime, LocalTime lunchEndTime) {
            UUID id = UUID.randomUUID();
            LunchRound round = new LunchRound(id, owner, title, room, hash, RoundStatus.OPEN, null, 0, Instant.now(), Instant.now(), drawAt, officeAddress, lunchStartTime, lunchEndTime);
            rounds.put(id, round); entries.put(id, new ArrayList<>()); return round;
        }
        public Optional<LunchRound> find(UUID id) { return Optional.ofNullable(rounds.get(id)); }
        public List<LunchRound> findDueOpen(Instant now) { return rounds.values().stream().filter(r -> r.status() == RoundStatus.OPEN && r.drawAt() != null && !r.drawAt().isAfter(now)).toList(); }
        public List<MenuEntry> entries(UUID id) { return List.copyOf(entries.get(id)); }
        public MenuEntry addEntry(UUID rid, UUID pid, String name, String menu) {
            List<MenuEntry> list = entries.get(rid);
            Optional<MenuEntry> existing = list.stream().filter(e -> e.participantId().equals(pid) && e.menu().equalsIgnoreCase(menu)).findFirst();
            if (existing.isPresent()) return existing.get();
            MenuEntry entry = new MenuEntry(UUID.randomUUID(), rid, pid, name, menu, Instant.now(), Instant.now());
            list.add(entry); return entry;
        }
        public void deleteEntry(UUID rid, UUID pid) { entries.get(rid).removeIf(e -> e.participantId().equals(pid)); }
        public void deleteEntry(UUID rid, UUID pid, UUID entryId) { entries.get(rid).removeIf(e -> e.participantId().equals(pid) && e.id().equals(entryId)); }
        public List<String> candidates(UUID id) { return entries.get(id).stream().map(e -> e.menu().strip()).distinct().toList(); }
        public boolean draw(UUID id, String winner, long version, boolean reroll, Instant drawnAt) {
            LunchRound r = rounds.get(id);
            if (r.version() != version || r.status() != (reroll ? RoundStatus.DRAWN : RoundStatus.OPEN)) return false;
            rounds.put(id, new LunchRound(id, r.ownerId(), r.title(), r.roomName(), r.joinTokenHash(), RoundStatus.DRAWN, winner, version + 1, r.createdAt(), Instant.now(), drawnAt, r.officeAddress(), r.lunchStartTime(), r.lunchEndTime()));
            drawCount++; return true;
        }
        public boolean close(UUID id, long version) {
            LunchRound r = rounds.get(id);
            if (r.version() != version || r.status() == RoundStatus.CLOSED) return false;
            rounds.put(id, new LunchRound(id, r.ownerId(), r.title(), r.roomName(), r.joinTokenHash(), RoundStatus.CLOSED, r.winner(), version + 1, r.createdAt(), Instant.now(), r.drawAt(), r.officeAddress(), r.lunchStartTime(), r.lunchEndTime()));
            return true;
        }
        public boolean updateSettings(UUID id, String address, LocalTime start, LocalTime end, long version) {
            LunchRound r = rounds.get(id);
            if (r.version() != version) return false;
            rounds.put(id, new LunchRound(id, r.ownerId(), r.title(), r.roomName(), r.joinTokenHash(), r.status(), r.winner(), version + 1, r.createdAt(), Instant.now(), r.drawAt(), address, start, end));
            return true;
        }
        public boolean resetForNewDay(UUID id, Instant drawAt, long version) {
            LunchRound r = rounds.get(id);
            if (r.version() != version) return false;
            rounds.put(id, new LunchRound(id, r.ownerId(), r.title(), r.roomName(), r.joinTokenHash(), RoundStatus.OPEN, null, version + 1, r.createdAt(), Instant.now(), drawAt, r.officeAddress(), r.lunchStartTime(), r.lunchEndTime()));
            entries.get(id).clear();
            return true;
        }
    }
}
