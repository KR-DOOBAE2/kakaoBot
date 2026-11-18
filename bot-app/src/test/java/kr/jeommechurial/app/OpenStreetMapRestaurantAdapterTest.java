package kr.jeommechurial.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

class OpenStreetMapRestaurantAdapterTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void usesVerifiedBojeonBuildingLocationForRestaurantRadius() {
        var adapter = new OpenStreetMapRestaurantAdapter(RestClient.builder(),
                "https://nominatim.openstreetmap.org", "https://overpass-api.de");
        var location = adapter.geocode("강남구 언주로 725");
        assertEquals(37.519313338214, location.latitude(), 0.000001);
        assertEquals(127.03449892281, location.longitude(), 0.000001);
        assertEquals(location.latitude(), adapter.searchAddresses("서울 강남구 언주로 725").getFirst().latitude());
        var nearby = OpenStreetMapRestaurantAdapter.addVerifiedPlaces(
                java.util.List.of(), location.latitude(), location.longitude(), 1000);
        assertEquals("무명떡볶이 본점", nearby.getFirst().name());
        assertTrue(nearby.getFirst().distanceMeters() < 300);
        assertTrue(OpenStreetMapRestaurantAdapter.addVerifiedPlaces(
                java.util.List.of(), 37.4, 127.0, 1000).isEmpty());
    }

    @Test void readsNamedRestaurantsAndFiltersPlacesOutsideRadius() throws Exception {
        var response = mapper.readTree("""
            {"elements":[
              {"type":"node","id":1,"lat":37.5,"lon":127.0,"tags":{"name":"가까운 식당","amenity":"restaurant"}},
              {"type":"way","id":2,"center":{"lat":37.5005,"lon":127.0},"tags":{"name":"건물 식당","amenity":"restaurant"}},
              {"type":"node","id":3,"lat":37.7,"lon":127.0,"tags":{"name":"너무 먼 식당","amenity":"restaurant"}},
              {"type":"node","id":4,"lat":37.5,"lon":127.0,"tags":{"amenity":"restaurant"}}
            ]}
            """);
        var result = OpenStreetMapRestaurantAdapter.parseRestaurants(response, 37.5, 127.0, 1000);
        assertEquals(2, result.size());
        assertEquals("osm-node-1", result.get(0).placeId());
        assertEquals("osm-way-2", result.get(1).placeId());
        assertTrue(result.get(1).distanceMeters() > 0);
        assertEquals("https://www.openstreetmap.org/way/2", result.get(1).placeUrl());
    }
}

