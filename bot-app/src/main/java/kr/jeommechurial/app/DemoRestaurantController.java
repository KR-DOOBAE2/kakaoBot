package kr.jeommechurial.app;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import kr.jeommechurial.core.PlaceSearchResult;
import kr.jeommechurial.service.RestaurantSearch;
import kr.jeommechurial.service.RoundRestaurantService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoRestaurantController {
    private static final int RADIUS_METERS = 1000;
    private static final Duration CACHE_DURATION = Duration.ofMinutes(10);
    private final RestaurantSearch search;
    private final Map<Query, CachedResult> cache = new ConcurrentHashMap<>();

    public DemoRestaurantController(RestaurantSearch search) {
        this.search = search;
    }

    @PostMapping("/api/demo/restaurants")
    public Recommendation recommend(@RequestBody Request request) {
        if (request == null || request.address() == null || request.address().isBlank()
                || request.address().length() > 160 || request.menu() == null || request.menu().isBlank()
                || request.menu().length() > 80) {
            throw new IllegalArgumentException("회사 주소와 당첨 메뉴를 확인해주세요.");
        }
        Query query = new Query(request.address().strip(), request.menu().strip());
        CachedResult cached = cache.get(query);
        if (cached != null && Instant.now().isBefore(cached.createdAt().plus(CACHE_DURATION))) {
            return cached.recommendation();
        }
        Recommendation result = find(query);
        if (cache.size() >= 100) cache.clear();
        cache.put(query, new CachedResult(Instant.now(), result));
        return result;
    }

    private Recommendation find(Query query) {
        RestaurantSearch.Location location = search.geocode(query.address());
        List<PlaceSearchResult> nearby = search.nearbyRestaurantsForMenu(
                location.latitude(), location.longitude(), RADIUS_METERS, query.menu());
        List<PlaceSearchResult> matching = nearby.stream()
                .filter(place -> RoundRestaurantService.matches(
                        query.menu(), place.name(), place.category())).toList();
        List<Place> places = matching.stream()
                .sorted(Comparator.comparingInt(PlaceSearchResult::distanceMeters))
                .limit(5)
                .map(place -> new Place(place.name(), place.address(), place.distanceMeters(),
                        place.placeUrl()))
                .toList();
        return new Recommendation(query.menu(), !matching.isEmpty(), RADIUS_METERS, places);
    }

    public record Request(String address, String menu) {}
    public record Place(String name, String address, int distanceMeters, String placeUrl) {}
    public record Recommendation(String winningMenu, boolean menuMatched, int radiusMeters,
                                 List<Place> restaurants) {}
    private record Query(String address, String menu) {}
    private record CachedResult(Instant createdAt, Recommendation recommendation) {}
}

