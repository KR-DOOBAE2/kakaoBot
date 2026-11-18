package kr.jeommechurial.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.jeommechurial.core.PlaceSearchResult;
import kr.jeommechurial.service.RestaurantSearch;
import org.junit.jupiter.api.Test;

class MenuSuggestionControllerTest {
    private final UUID room = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final LocalDate day = LocalDate.of(2026, 11, 18);
    private final Map<String, String> foods = Map.ofEntries(
            Map.entry("김치찌개", "/food-art/kimchi-jjigae.png"),
            Map.entry("된장찌개", "/food-art/doenjang-jjigae.png"),
            Map.entry("돈까스", "/food-art/donkatsu.png"),
            Map.entry("비빔밥", "/food-art/bibimbap.png"),
            Map.entry("불고기", "/food-art/bulgogi.png"),
            Map.entry("제육볶음", "/food-art/jeyuk-bokkeum.png"),
            Map.entry("카레라이스", "/food-art/curry-rice.png"),
            Map.entry("라멘", "/food-art/ramen.png"),
            Map.entry("칼국수", "/food-art/kalguksu.png"));

    @Test
    void storeRecommendationsOnlyIncludeMealsWithMatchingRestaurantsInsideOneKilometer() {
        FakeSearch search = new FakeSearch();
        var result = MenuSuggestionController.select(room, "서울시청", day, "cloudy", "store", true, foods, search);

        assertThat(result.suggestions()).hasSize(3);
        assertThat(result.suggestions()).allSatisfy(item -> {
            assertThat(item.nearbyPlace()).isNotNull();
            assertThat(item.nearbyPlace().distanceMeters()).isEqualTo(800);
        });
        assertThat(search.requestedRadius).isEqualTo(1000);
        assertThat(search.geocodeCalls).isEqualTo(1);
    }

    @Test
    void deliveryUsesWeatherMenuPoolWithoutRestaurantDistanceSearch() {
        FakeSearch search = new FakeSearch();
        var result = MenuSuggestionController.select(room, "서울시청", day, "cloudy", "delivery", true, foods, search);

        assertThat(result.suggestions()).hasSize(3);
        assertThat(result.suggestions()).allSatisfy(item -> assertThat(item.nearbyPlace()).isNull());
        assertThat(search.geocodeCalls).isZero();
        assertThat(search.requestedRadius).isZero();
    }

    private static class FakeSearch implements RestaurantSearch {
        int requestedRadius;
        int geocodeCalls;

        @Override public Location geocode(String address) {
            assertThat(address).isEqualTo("서울시청");
            geocodeCalls++;
            return new Location(37.56, 126.98);
        }

        @Override public List<PlaceSearchResult> nearbyRestaurants(double lat, double lon, int radius) {
            return List.of();
        }

        @Override public List<PlaceSearchResult> nearbyRestaurantsMatchingMenu(double lat, double lon,
                                                                                int radius, String menu) {
            requestedRadius = radius;
            return List.of(place(menu + " 먼 식당", 1200), place(menu + " 가까운 식당", 800));
        }

        private PlaceSearchResult place(String name, int distance) {
            return new PlaceSearchResult(name, name, "주소", "restaurant", "",
                    "https://www.openstreetmap.org/", 37.56, 126.98, distance);
        }
    }
}
