package kr.jeommechurial.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenStreetMapRestaurantAdapterTest {
    private final ObjectMapper mapper = new ObjectMapper();

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
