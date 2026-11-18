package kr.jeommechurial.app;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import kr.jeommechurial.core.PlaceSearchResult;
import kr.jeommechurial.core.RoundView;
import kr.jeommechurial.persistence.FoodCatalogEntity;
import kr.jeommechurial.persistence.FoodCatalogJpa;
import kr.jeommechurial.service.LunchService;
import kr.jeommechurial.service.RestaurantSearch;
import kr.jeommechurial.service.RoundRestaurantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rounds/{id}/menu-suggestions")
public class MenuSuggestionController {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int RADIUS_METERS = 1000;
    private static final Map<String, List<String>> WEATHER_MENUS = Map.of(
            "sunny", List.of("냉면", "비빔냉면", "초밥", "샐러드", "김밥", "쌀국수", "돈까스", "샌드위치", "비빔밥"),
            "cloudy", List.of("김치찌개", "된장찌개", "돈까스", "비빔밥", "불고기", "제육볶음", "카레라이스", "라멘", "칼국수"),
            "rain", List.of("칼국수", "김치찌개", "짬뽕", "순두부찌개", "우동", "부대찌개", "만둣국", "수육", "라멘"),
            "snow", List.of("만둣국", "갈비탕", "설렁탕", "순대국밥", "육개장", "순두부찌개", "라멘", "김치찌개", "감자탕"));

    private final LunchService rounds;
    private final TeamWeatherController weather;
    private final RestaurantSearch places;
    private final FoodCatalogJpa catalog;
    private final Map<CacheKey, Cached> cache = new ConcurrentHashMap<>();

    public MenuSuggestionController(LunchService rounds, TeamWeatherController weather,
                                    RestaurantSearch places, FoodCatalogJpa catalog) {
        this.rounds = rounds;
        this.weather = weather;
        this.places = places;
        this.catalog = catalog;
    }

    @GetMapping
    public Recommendation recommend(@PathVariable UUID id, @RequestParam String token,
                                    @RequestParam(defaultValue = "store") String mode) {
        if (!mode.equals("store") && !mode.equals("delivery"))
            throw new IllegalArgumentException("매장 또는 배달을 선택해주세요.");
        RoundView round = rounds.get(id, token);
        if (round.officeAddress() == null || round.officeAddress().isBlank())
            throw new IllegalStateException("회사 주소를 먼저 설정해주세요.");
        String theme;
        boolean weatherAvailable;
        try {
            theme = weather.current(id, token).theme();
            weatherAvailable = true;
        } catch (RuntimeException unavailable) {
            theme = "cloudy";
            weatherAvailable = false;
        }
        LocalDate date = LocalDate.now(SEOUL);
        CacheKey key = new CacheKey(id, round.officeAddress(), date, theme, mode, weatherAvailable);
        Cached cached = cache.get(key);
        if (cached != null && Instant.now().isBefore(cached.expiresAt())) return cached.result();
        Recommendation result = choose(round, date, theme, mode, weatherAvailable);
        if (cache.size() >= 100) cache.clear();
        cache.put(key, new Cached(result, Instant.now().plus(Duration.ofHours(3))));
        return result;
    }

    private Recommendation choose(RoundView round, LocalDate date, String theme,
                                  String mode, boolean weatherAvailable) {
        Map<String, String> foods = catalog.findAllByOrderBySortOrderAsc().stream()
                .collect(Collectors.toMap(FoodCatalogEntity::getName, FoodCatalogEntity::getImagePath));
        return select(round.id(), round.officeAddress(), date, theme, mode, weatherAvailable, foods, places);
    }

    static Recommendation select(UUID roundId, String address, LocalDate date, String theme,
                                 String mode, boolean weatherAvailable, Map<String, String> foods,
                                 RestaurantSearch places) {
        List<String> choices = new ArrayList<>(WEATHER_MENUS.getOrDefault(theme, WEATHER_MENUS.get("cloudy")));
        Collections.shuffle(choices, new Random(Objects.hash(roundId, date, theme, mode)));
        List<Suggestion> suggestions = new ArrayList<>();
        RestaurantSearch.Location location = mode.equals("store") ? places.geocode(address) : null;
        for (String name : choices) {
            String imagePath = foods.get(name);
            if (imagePath == null) continue;
            if (mode.equals("delivery")) {
                suggestions.add(new Suggestion(name, imagePath, null));
            } else {
                List<PlaceSearchResult> matched = places.nearbyRestaurantsMatchingMenu(
                                location.latitude(), location.longitude(), RADIUS_METERS, name).stream()
                        .filter(place -> place.distanceMeters() <= RADIUS_METERS)
                        .filter(place -> RoundRestaurantService.matches(name, place.name(), place.category()))
                        .sorted(Comparator.comparingInt(PlaceSearchResult::distanceMeters)).toList();
                if (!matched.isEmpty()) {
                    PlaceSearchResult nearest = matched.getFirst();
                    suggestions.add(new Suggestion(name, imagePath,
                            new NearbyPlace(nearest.name(), nearest.distanceMeters(), nearest.placeUrl())));
                }
            }
            if (suggestions.size() == 3) break;
        }
        return new Recommendation(theme, weatherAvailable, mode, RADIUS_METERS, List.copyOf(suggestions));
    }

    public record NearbyPlace(String name, int distanceMeters, String placeUrl) {}
    public record Suggestion(String name, String imagePath, NearbyPlace nearbyPlace) {}
    public record Recommendation(String weatherTheme, boolean weatherAvailable, String mode,
                                 int radiusMeters, List<Suggestion> suggestions) {}
    private record CacheKey(UUID roundId, String address, LocalDate date, String theme,
                            String mode, boolean weatherAvailable) {}
    private record Cached(Recommendation result, Instant expiresAt) {}
}
