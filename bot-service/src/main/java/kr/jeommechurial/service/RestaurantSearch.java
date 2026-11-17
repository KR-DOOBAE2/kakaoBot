package kr.jeommechurial.service;

import kr.jeommechurial.core.PlaceSearchResult;
import java.util.List;

public interface RestaurantSearch {
    Location geocode(String address);
    List<PlaceSearchResult> nearbyRestaurants(double latitude, double longitude, int radiusMeters);
    default List<PlaceSearchResult> nearbyRestaurantsForMenu(double latitude, double longitude,
                                                            int radiusMeters, String menu) {
        return nearbyRestaurants(latitude, longitude, radiusMeters);
    }
    record Location(double latitude, double longitude) {}
}
