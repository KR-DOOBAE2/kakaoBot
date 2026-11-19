package kr.jeommechurial.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kr.jeommechurial.core.PlaceSearchResult;
import kr.jeommechurial.service.RestaurantSearch;
import kr.jeommechurial.service.MenuPriceLookup;
import org.junit.jupiter.api.Test;

class DemoRestaurantControllerTest {
    @Test
    void doesNotRecommendTteokbokkiForGrilledFish() {
        FakeSearch search = new FakeSearch();
        var result = new DemoRestaurantController(search, (name, address, office, menu) -> java.util.Optional.empty()).recommend(
                new DemoRestaurantController.Request("서울시청", "생선구이"));
        assertThat(result.menuMatched()).isFalse();
        assertThat(result.restaurants()).isEmpty();
    }

    @Test
    void recommendsNearbyRestaurantsMatchingTheDrawnMenu() {
        FakeSearch search = new FakeSearch();

        var result = new DemoRestaurantController(search, (name, address, office, menu) -> java.util.Optional.empty()).recommend(
                new DemoRestaurantController.Request("서울시청", "김치찌개"));

        assertThat(result.menuMatched()).isTrue();
        assertThat(result.radiusMeters()).isEqualTo(1000);
        assertThat(result.restaurants()).extracting(DemoRestaurantController.Place::name)
                .containsExactly("김치찌개집", "한식당");
        assertThat(search.requestedRadius).isEqualTo(1000);
        assertThat(search.requestedMenu).isEqualTo("김치찌개");
    }

    @Test
    void prioritizesVerifiedAffordableMenuAndExcludesVerifiedOverBudget() {
        MenuPriceLookup prices = (name, address, office, menu) -> switch (name) {
            case "한식당" -> java.util.Optional.of(new MenuPriceLookup.Price("김치찌개", 9000, "공공데이터", "2026-09-30"));
            case "김치찌개집" -> java.util.Optional.of(new MenuPriceLookup.Price("김치찌개", 23000, "공공데이터", "2026-09-30"));
            default -> java.util.Optional.empty();
        };
        var result = new DemoRestaurantController(new FakeSearch(), prices).recommend(
                new DemoRestaurantController.Request("서울시청", "김치찌개"));
        assertThat(result.restaurants()).extracting(DemoRestaurantController.Place::name)
                .containsExactly("한식당");
        assertThat(result.restaurants().get(0).price().won()).isEqualTo(9000);
    }

    private static PlaceSearchResult place(String name, String category, int distance) {
        return new PlaceSearchResult(name, name, "주소", category, "",
                "https://www.openstreetmap.org/", 37.56, 126.98, distance);
    }

    private static class FakeSearch implements RestaurantSearch {
        private int requestedRadius;
        private String requestedMenu;

        @Override
        public Location geocode(String address) {
            assertThat(address).isEqualTo("서울시청");
            return new Location(37.56, 126.98);
        }

        @Override
        public List<PlaceSearchResult> nearbyRestaurants(double latitude, double longitude, int radiusMeters) {
            requestedRadius = radiusMeters;
            return List.of(place("치킨집", "chicken", 100), place("한식당", "korean", 450),
                    place("김치찌개집", "korean", 250));
        }

        @Override
        public List<PlaceSearchResult> nearbyRestaurantsForMenu(double latitude, double longitude,
                                                                 int radiusMeters, String menu) {
            requestedMenu = menu;
            return nearbyRestaurants(latitude, longitude, radiusMeters);
        }
    }
}

