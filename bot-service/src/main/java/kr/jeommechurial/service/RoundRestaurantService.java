package kr.jeommechurial.service;

import kr.jeommechurial.core.MenuEntry;
import kr.jeommechurial.core.PlaceCandidate;
import kr.jeommechurial.core.RoundView;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoundRestaurantService {
    private final LunchService rounds;
    private final RestaurantSearch search;
    private final OfficeRepository restaurants;
    private final Map<CacheKey, Recommendation> cache = new ConcurrentHashMap<>();

    public RoundRestaurantService(LunchService rounds, RestaurantSearch search, OfficeRepository restaurants) {
        this.rounds = rounds;
        this.search = search;
        this.restaurants = restaurants;
    }

    public Recommendation recommend(UUID roundId, String joinToken) {
        RoundView round = rounds.get(roundId, joinToken);
        if (round.winner() == null) throw new IllegalStateException("룰렛 결과가 나온 뒤 식당을 추천할 수 있어요.");
        if (round.officeAddress() == null || round.officeAddress().isBlank())
            throw new IllegalStateException("이 방에는 회사 주소가 없어요. 주소를 입력한 새 점심방을 만들어주세요.");
        return cache.computeIfAbsent(new CacheKey(roundId, round.drawAt(), round.officeAddress(), round.winner()), ignored -> find(round));
    }

    private Recommendation find(RoundView round) {
        RestaurantSearch.Location location = search.geocode(round.officeAddress());
        List<PlaceCandidate> places = restaurants.discover(search.nearbyRestaurantsForMenu(
                location.latitude(), location.longitude(), 1000, round.winner()));
        if (places.isEmpty()) throw new IllegalStateException("회사 주변 1km에서 식당을 찾지 못했어요. 주소를 확인해주세요.");
        List<PlaceCandidate> matching = places.stream()
                .filter(p -> matches(round.winner(), p.name(), p.category())).toList();
        List<Restaurant> ranked = (matching.isEmpty() ? places : matching).stream().map(p -> new Restaurant(
                p.placeId(), p.name(), p.address(), p.category(), p.placeUrl(), p.distanceMeters(),
                p.newlyDiscovered(), (int) round.entries().stream().map(MenuEntry::menu)
                        .filter(menu -> matches(menu, p.name(), p.category())).count(), p.score()))
                .sorted(Comparator.comparing(Restaurant::recentlyFound).reversed()
                        .thenComparing(Comparator.comparingInt(Restaurant::menuMentions).reversed())
                        .thenComparingInt(Restaurant::distanceMeters)
                        .thenComparing(Comparator.comparingInt(Restaurant::ratingScore).reversed())
                        .thenComparing(Restaurant::name))
                .limit(5).toList();
        return new Recommendation(round.winner(), !matching.isEmpty(), ranked.get(0), ranked);
    }

    public static boolean matches(String menu, String name, String category) {
        String m = normalize(menu), place = normalize(name + " " + category);
        if (m.length() >= 2 && place.contains(m)) return true;
        for (var group : CUISINES.entrySet()) {
            if (group.getValue().stream().anyMatch(m::contains)
                    && group.getValue().stream().anyMatch(place::contains)) return true;
        }
        return false;
    }

    public static List<String> searchTerms(String menu) {
        String cleaned = menu.replaceFirst("^(매장|배달)\\s*·\\s*", "").strip();
        if (cleaned.contains("떡볶이") || cleaned.contains("라볶이"))
            return List.of(cleaned, "분식");
        return List.of(cleaned);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFKC)
                .replaceAll("[\\s\\p{Punct}]", "");
    }

    private static final Map<String, List<String>> CUISINES = Map.of(
            "korean", List.of("한식", "korean", "김치", "찌개", "국밥", "비빔밥", "백반", "불고기"),
            "japanese", List.of("일식", "japanese", "초밥", "스시", "라멘", "돈카츠", "돈까스", "우동"),
            "chinese", List.of("중식", "chinese", "짜장", "짬뽕", "마라", "탕수육"),
            "western", List.of("양식", "italian", "western", "파스타", "피자", "스테이크"),
            "bunsik", List.of("분식", "떡볶이", "라볶이"),
            "fastfood", List.of("패스트푸드", "fastfood", "burger", "햄버거", "샌드위치"));

    public record Restaurant(String placeId, String name, String address, String category, String placeUrl,
                             int distanceMeters, boolean recentlyFound, int menuMentions, int ratingScore) {}
    public record Recommendation(String winningMenu, boolean menuMatched, Restaurant topPick, List<Restaurant> restaurants) {}
    private record CacheKey(UUID roundId, Instant drawAt, String address, String winner) {}
}

