package kr.jeommechurial.service;

import kr.jeommechurial.core.PlaceCandidate;
import kr.jeommechurial.core.PlaceSearchResult;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoundRestaurantServiceTest {
    @Test void tteokbokkiMatchesBunsikPlacesAndSearchesBothNames() {
        assertTrue(RoundRestaurantService.matches("매장 · 떡볶이", "무명떡볶이", "restaurant"));
        assertTrue(RoundRestaurantService.matches("떡볶이", "우리동네분식", "fast_food"));
        assertFalse(RoundRestaurantService.matches("떡볶이", "초밥집", "japanese"));
        assertEquals(List.of("떡볶이", "분식"), RoundRestaurantService.searchTerms("매장 · 떡볶이"));
    }
    @Test void grilledFishDoesNotMatchTteokbokkiAndSearchesRelatedTerms() {
        assertFalse(RoundRestaurantService.matches("생선구이", "무명떡볶이", "분식"));
        assertTrue(RoundRestaurantService.matches("생선구이", "고등어구이 전문점", "restaurant"));
        assertTrue(RoundRestaurantService.searchTerms("생선구이").contains("고등어구이"));
    }
    @Test void returnsEmptyRecommendationWhenOnlyUnrelatedPlacesExist() {
        var rounds = new LunchServiceTest.FakeRepository();
        var lunch = new LunchService(rounds, "secret", Clock.fixed(Instant.parse("2026-11-16T01:00:00Z"), ZoneOffset.UTC));
        var created = lunch.create("점심", "팀", "서울시청", LocalTime.NOON, LocalTime.of(13, 0));
        lunch.register(created.id(), created.joinToken(), UUID.randomUUID(), null, "생선구이");
        var due = new LunchService(rounds, "secret", Clock.fixed(Instant.parse("2026-11-16T02:30:00Z"), ZoneOffset.UTC));
        var catalog = new OfficeLunchServiceTest.FakeOfficeRepository() {
            @Override public List<PlaceCandidate> discover(List<PlaceSearchResult> ignored) {
                return List.of(new PlaceCandidate("tteokbokki", "무명떡볶이", "주소", "분식", "",
                        "https://example.com", 37.5, 127, 200, Instant.now(), 0, 0));
            }
        };
        var result = new RoundRestaurantService(due, search(), catalog, noPrices())
                .recommend(created.id(), created.joinToken());
        assertFalse(result.menuMatched());
        assertNull(result.topPick());
        assertTrue(result.restaurants().isEmpty());
    }
    @Test void requiresDrawAndValidParticipationLink() {
        var rounds = new LunchServiceTest.FakeRepository();
        var lunch = new LunchService(rounds, "secret", Clock.fixed(Instant.parse("2026-11-16T01:00:00Z"), ZoneOffset.UTC));
        var created = lunch.create("점심", "팀", "서울시청", LocalTime.NOON, LocalTime.of(13, 0));
        var service = new RoundRestaurantService(lunch, search(), new OfficeLunchServiceTest.FakeOfficeRepository(), noPrices());
        assertThrows(SecurityException.class, () -> service.recommend(created.id(), "wrong"));
        assertThrows(IllegalStateException.class, () -> service.recommend(created.id(), created.joinToken()));
    }

    @Test void recommendsRecentlyFoundThenMenuMentionsAndCachesResult() {
        var rounds = new LunchServiceTest.FakeRepository();
        var lunch = new LunchService(rounds, "secret", Clock.fixed(Instant.parse("2026-11-16T01:00:00Z"), ZoneOffset.UTC));
        var created = lunch.create("점심", "팀", "서울시청", LocalTime.NOON, LocalTime.of(13, 0));
        lunch.register(created.id(), created.joinToken(), UUID.randomUUID(), null, "김치찌개");
        lunch.register(created.id(), created.joinToken(), UUID.randomUUID(), null, "김치찌개");
        var due = new LunchService(rounds, "secret", Clock.fixed(Instant.parse("2026-11-16T02:30:00Z"), ZoneOffset.UTC));
        var catalog = new OfficeLunchServiceTest.FakeOfficeRepository() {
            @Override public List<PlaceCandidate> discover(List<PlaceSearchResult> ignored) {
                Instant now = Instant.now();
                return List.of(
                        new PlaceCandidate("old", "오래된 한식집", "주소", "korean", "", "https://example.com/old", 37.5, 127, 10, now.minus(Duration.ofDays(30)), 20, 0),
                        new PlaceCandidate("new-far", "새 한식집", "주소", "korean", "", "https://example.com/new", 37.5, 127, 300, now, 0, 0),
                        new PlaceCandidate("new-near", "새 일식집", "주소", "japanese", "", "https://example.com/near", 37.5, 127, 50, now, 0, 0));
            }
        };
        var service = new RoundRestaurantService(due, search(), catalog, noPrices());
        var result = service.recommend(created.id(), created.joinToken());
        assertEquals("new-far", result.topPick().placeId());
        assertEquals(2, result.topPick().menuMentions());
        assertTrue(result.menuMatched());
        assertEquals(List.of("new-far", "old"), result.restaurants().stream().map(RoundRestaurantService.Restaurant::placeId).toList());
        assertSame(result, service.recommend(created.id(), created.joinToken()));
    }

    private static MenuPriceLookup noPrices() { return (name, address, office, menu) -> java.util.Optional.empty(); }

    private static RestaurantSearch search() {
        return new RestaurantSearch() {
            public Location geocode(String address) { return new Location(37.5, 127); }
            public List<PlaceSearchResult> nearbyRestaurants(double lat, double lon, int radius) {
                return List.of(new PlaceSearchResult("dummy", "식당", "주소", "", "", "https://example.com", lat, lon, 100));
            }
        };
    }
}

